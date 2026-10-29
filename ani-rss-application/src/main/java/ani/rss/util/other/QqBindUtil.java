package ani.rss.util.other;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QQ 用户 OpenID 自动绑定
 * <p>
 * C2C OpenID 无法通过 REST 接口查询，只能从 QQ 网关事件中获取：
 * 私聊机器人（或添加好友）即可获得用户 OpenID。
 * 绑定过程放在后台虚拟线程，前端通过任务状态轮询拿到验证码和最终结果。
 */
@Slf4j
public class QqBindUtil {

    /**
     * 1 << 25，单聊事件
     */
    private static final int INTENTS = 33554432;

    private static final long BIND_TIMEOUT_MILLIS = 60_000;
    private static final int HEARTBEAT_DEFAULT = 45_000;

    private static final Map<String, Task> TASKS = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService CLEANER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "qq-bind-cleaner");
        thread.setDaemon(true);
        return thread;
    });

    public enum State {
        CONNECTING,
        WAITING,
        SUCCESS,
        ERROR,
        CANCELLED
    }

    @Data
    @Accessors(chain = true)
    public static class Session {
        private String taskId;
        private String verifyCode;
        private String state;
        private String message;
        private String openId;
    }

    private static class Task {
        private final String taskId;
        private final String verifyCode;
        private volatile State state = State.CONNECTING;
        private volatile String message = "正在连接 QQ 网关…";
        private volatile String openId;
        private volatile WebSocket socket;
        private volatile boolean cancelled;

        private Task(String taskId, String verifyCode) {
            this.taskId = taskId;
            this.verifyCode = verifyCode;
        }

        private Session snapshot() {
            return new Session()
                    .setTaskId(taskId)
                    .setVerifyCode(verifyCode)
                    .setState(state.name())
                    .setMessage(message)
                    .setOpenId(openId);
        }
    }

    /**
     * 开始绑定任务
     *
     * @param appId        QQ 开放平台 AppID
     * @param clientSecret QQ 开放平台 AppSecret
     * @return 任务状态
     */
    public static Session start(String appId, String clientSecret) {
        if (StrUtil.isBlank(appId)) {
            throw new IllegalArgumentException("QQ AppID 为空");
        }
        if (StrUtil.isBlank(clientSecret)) {
            throw new IllegalArgumentException("QQ AppSecret 为空");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        Task task = new Task(taskId, generateVerifyCode());
        TASKS.put(taskId, task);
        CLEANER.schedule(() -> TASKS.remove(taskId), 10, TimeUnit.MINUTES);

        Thread.ofVirtual().name("qq-bind-" + taskId).start(() -> bind(task, appId, clientSecret));
        return task.snapshot();
    }

    /**
     * 查询绑定任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    public static Session get(String taskId) {
        return getTask(taskId).snapshot();
    }

    /**
     * 取消绑定任务
     *
     * @param taskId 任务 ID
     * @return 任务状态
     */
    public static Session cancel(String taskId) {
        Task task = getTask(taskId);
        task.cancelled = true;
        task.state = State.CANCELLED;
        task.message = "已取消绑定";

        WebSocket socket = task.socket;
        if (socket != null) {
            try {
                socket.abort();
            } catch (Exception ignored) {
                // 连接可能已经关闭
            }
        }
        return task.snapshot();
    }

    private static Task getTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("绑定任务 ID 为空");
        }
        Task task = TASKS.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("绑定任务不存在或已过期，请重试");
        }
        return task;
    }

    private static void bind(Task task, String appId, String clientSecret) {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MILLIS;
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "qq-bind-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        WebSocket socket = null;
        HttpClient client = null;

        try {
            String token = QqUtil.getAccessToken(appId, clientSecret);
            checkCancelled(task);

            String gatewayUrl = QqUtil.getGatewayUrl(token);
            checkCancelled(task);

            CompletableFuture<Integer> hello = new CompletableFuture<>();
            CompletableFuture<Void> ready = new CompletableFuture<>();
            CompletableFuture<String> openId = new CompletableFuture<>();
            AtomicLong sequence = new AtomicLong();

            client = buildHttpClient();
            socket = client.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(gatewayUrl), new GatewayListener(task, hello, ready, openId, sequence))
                    .get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
            task.socket = socket;
            checkCancelled(task);

            int heartbeatInterval = hello.get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
            startHeartbeat(heartbeat, socket, heartbeatInterval, sequence);

            send(socket, buildIdentify(token));
            checkCancelled(task);

            ready.get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
            checkCancelled(task);

            task.state = State.WAITING;
            task.message = StrFormatter.format("请用手机 QQ 私聊机器人，发送验证码 [{}]", task.verifyCode);

            task.openId = openId.get(remainingMillis(deadline), TimeUnit.MILLISECONDS);
            task.state = State.SUCCESS;
            task.message = "绑定成功";
        } catch (TimeoutException e) {
            if (!task.cancelled) {
                task.state = State.ERROR;
                task.message = "绑定超时，请在 60 秒内发送验证码";
            }
        } catch (CancellationException e) {
            task.state = State.CANCELLED;
            task.message = "已取消绑定";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.state = State.CANCELLED;
            task.message = "已取消绑定";
        } catch (ExecutionException e) {
            if (task.cancelled) {
                task.state = State.CANCELLED;
                task.message = "已取消绑定";
            } else {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                task.state = State.ERROR;
                task.message = "绑定失败：" + messageOf(cause);
                log.warn("QQ 绑定失败", cause);
            }
        } catch (Exception e) {
            if (!task.cancelled) {
                task.state = State.ERROR;
                task.message = "绑定失败：" + messageOf(e);
                log.warn("QQ 绑定失败", e);
            }
        } finally {
            heartbeat.shutdownNow();
            close(socket);
            close(client);
        }
    }

    private static void checkCancelled(Task task) {
        if (task.cancelled) {
            throw new CancellationException("已取消绑定");
        }
    }

    private static long remainingMillis(long deadline) {
        return Math.max(deadline - System.currentTimeMillis(), 1);
    }

    private static String generateVerifyCode() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1);

        Config config = ConfigUtil.CONFIG;
        Integer proxyPort = config.getProxyPort();
        if (Boolean.TRUE.equals(config.getProxy())
                && HttpReq.isProxy(QqUtil.GATEWAY_URL)
                && StrUtil.isNotBlank(config.getProxyHost())
                && proxyPort != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), proxyPort)));
        }

        return builder.build();
    }

    private static String buildIdentify(String token) {
        JsonObject data = new JsonObject();
        data.addProperty("token", "QQBot " + token);
        data.addProperty("intents", INTENTS);

        JsonArray shard = new JsonArray();
        shard.add(0);
        shard.add(1);
        data.add("shard", shard);

        JsonObject payload = new JsonObject();
        payload.addProperty("op", 2);
        payload.add("d", data);
        return payload.toString();
    }

    private static void startHeartbeat(ScheduledExecutorService executor, WebSocket socket, int interval, AtomicLong sequence) {
        int delay = interval > 0 ? interval : HEARTBEAT_DEFAULT;
        executor.scheduleAtFixedRate(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("op", 1);
                long seq = sequence.get();
                if (seq == 0) {
                    payload.add("d", null);
                } else {
                    payload.addProperty("d", seq);
                }
                send(socket, payload.toString());
            } catch (Exception e) {
                log.debug("QQ 心跳发送失败: {}", e.getMessage());
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    private static void send(WebSocket socket, String text) {
        synchronized (socket) {
            socket.sendText(text, true).join();
        }
    }

    private static void close(WebSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "").orTimeout(2, TimeUnit.SECONDS).join();
        } catch (Exception ignored) {
            // 连接可能已被取消或关闭
        }
    }

    private static void close(HttpClient client) {
        if (client == null) {
            return;
        }
        try {
            client.shutdownNow();
        } catch (Exception ignored) {
            // 关闭客户端失败不影响绑定结果
        }
    }

    private static String messageOf(Throwable throwable) {
        return StrUtil.blankToDefault(throwable.getMessage(), throwable.getClass().getSimpleName());
    }

    private static String getString(JsonObject object, String property) {
        if (object == null || !object.has(property) || !object.get(property).isJsonPrimitive()) {
            return null;
        }
        return object.get(property).getAsString();
    }

    private static class GatewayListener implements WebSocket.Listener {
        private final Task task;
        private final CompletableFuture<Integer> hello;
        private final CompletableFuture<Void> ready;
        private final CompletableFuture<String> openId;
        private final AtomicLong sequence;
        private final StringBuilder buffer = new StringBuilder();

        private GatewayListener(Task task,
                                CompletableFuture<Integer> hello,
                                CompletableFuture<Void> ready,
                                CompletableFuture<String> openId,
                                AtomicLong sequence) {
            this.task = task;
            this.hello = hello;
            this.ready = ready;
            this.openId = openId;
            this.sequence = sequence;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String payload = buffer.toString();
                buffer.setLength(0);
                try {
                    handle(payload);
                } catch (Exception e) {
                    fail(new RuntimeException(e.getMessage(), e));
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            fail(new RuntimeException("QQ 网关关闭了连接"));
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            fail(new RuntimeException(error.getMessage(), error));
        }

        private void fail(Throwable throwable) {
            RuntimeException exception = throwable instanceof RuntimeException
                    ? (RuntimeException) throwable
                    : new RuntimeException(throwable);
            hello.completeExceptionally(exception);
            ready.completeExceptionally(exception);
            openId.completeExceptionally(exception);
        }

        private void handle(String payload) {
            JsonObject root = GsonStatic.fromJson(payload, JsonObject.class);
            if (root == null || !root.has("op")) {
                return;
            }

            int op = root.get("op").getAsInt();

            if (op == 10) {
                int interval = HEARTBEAT_DEFAULT;
                if (root.has("d") && root.get("d").isJsonObject()) {
                    JsonObject data = root.getAsJsonObject("d");
                    if (data.has("heartbeat_interval")) {
                        interval = data.get("heartbeat_interval").getAsInt();
                    }
                }
                hello.complete(interval);
                return;
            }

            if (op == 9) {
                fail(new RuntimeException("QQ 机器人未开通单聊事件权限，请在开放平台申请"));
                return;
            }

            if (op != 0) {
                return;
            }

            if (root.has("s") && root.get("s").isJsonPrimitive()) {
                try {
                    sequence.set(root.get("s").getAsLong());
                } catch (Exception ignored) {
                }
            }

            if (!root.has("t")) {
                return;
            }

            String event = root.get("t").getAsString();
            if ("READY".equals(event)) {
                ready.complete(null);
                return;
            }

            if (!root.has("d") || !root.get("d").isJsonObject()) {
                return;
            }
            handleC2cEvent(event, root.getAsJsonObject("d"));
        }

        private void handleC2cEvent(String event, JsonObject data) {
            String verifyCode = task.verifyCode;

            if ("C2C_MESSAGE_CREATE".equals(event)) {
                JsonObject author = data.has("author") && data.get("author").isJsonObject()
                        ? data.getAsJsonObject("author")
                        : null;
                String userId = getString(author, "user_openid");
                String content = getString(data, "content");
                if (StrUtil.isNotBlank(userId) && content != null && content.contains(verifyCode)) {
                    openId.complete(userId);
                }
                return;
            }

            if ("FRIEND_ADD".equals(event)) {
                String userId = getString(data, "openid");
                if (StrUtil.isNotBlank(userId)) {
                    openId.complete(userId);
                }
            }
        }
    }
}
