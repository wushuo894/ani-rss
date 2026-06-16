package ani.rss.notification;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.web.ContentType;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonStreamParser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * OneBot
 */
@Slf4j
public class OneBotNotification implements BaseNotification {

    @Override
    public void test(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Boolean success = send(notificationConfig, ani, text, notificationStatusEnum);
        if (!success) {
            throw new IllegalStateException("OneBot 发送失败");
        }
    }

    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String oneBotUrl = notificationConfig.getOneBotUrl();
        String oneBotMessageType = notificationConfig.getOneBotMessageType();
        String oneBotUserId = notificationConfig.getOneBotUserId();
        String oneBotGroupId = notificationConfig.getOneBotGroupId();
        String oneBotAccessToken = notificationConfig.getOneBotAccessToken();

        if (StrUtil.isBlank(oneBotUrl)) {
            throw new IllegalArgumentException("OneBot 地址为空");
        }

        oneBotUrl = oneBotUrl.trim();
        oneBotMessageType = StrUtil.blankToDefault(oneBotMessageType, "PRIVATE").trim().toUpperCase();

        if (!List.of("PRIVATE", "GROUP").contains(oneBotMessageType)) {
            throw new IllegalArgumentException("OneBot 消息类型仅支持 PRIVATE 或 GROUP");
        }

        if ("PRIVATE".equals(oneBotMessageType) && StrUtil.isBlank(oneBotUserId)) {
            throw new IllegalArgumentException("OneBot 私聊模式用户 ID 为空");
        }
        if ("GROUP".equals(oneBotMessageType) && StrUtil.isBlank(oneBotGroupId)) {
            throw new IllegalArgumentException("OneBot 群聊模式群号为空");
        }

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);

        JsonObject params = buildParams(
                ani,
                notificationTemplate,
                oneBotMessageType,
                oneBotUserId,
                oneBotGroupId,
                notificationConfig.getOneBotImage()
        );

        if (StrUtil.startWithAnyIgnoreCase(oneBotUrl, "ws://", "wss://")) {
            return sendByWebSocket(oneBotUrl, oneBotAccessToken, params, oneBotMessageType);
        }
        if (StrUtil.startWithAnyIgnoreCase(oneBotUrl, "http://", "https://")) {
            return sendByHttp(oneBotUrl, oneBotAccessToken, params, oneBotMessageType);
        }

        throw new IllegalArgumentException("OneBot 地址必须以 http://、https://、ws:// 或 wss:// 开头");
    }

    private static Boolean sendByHttp(String oneBotUrl, String oneBotAccessToken, JsonObject params, String oneBotMessageType) {
        String actionName = getActionName(oneBotMessageType);
        String url = StrUtil.removeSuffix(oneBotUrl, "/") + "/" + actionName;

        HttpRequest request = HttpReq.post(url)
                .contentType(ContentType.JSON);
        if (StrUtil.isNotBlank(oneBotAccessToken)) {
            request.header("Authorization", getAuthorization(oneBotAccessToken));
        }

        return request.body(GsonStatic.toJson(params))
                .thenFunction(response -> {
                    if (!response.isOk()) {
                        throw new IllegalStateException(StrFormatter.format(
                                "OneBot HTTP 请求失败, status: {}, message: {}",
                                response.getStatus(),
                                getResponseMessage(response.body())
                        ));
                    }
                    assertOneBotResponse(response.body());
                    return true;
                });
    }

    private static Boolean sendByWebSocket(String oneBotUrl, String oneBotAccessToken, JsonObject params, String oneBotMessageType) {
        String actionName;
        try {
            actionName = getActionName(oneBotMessageType);

            JsonObject action = new JsonObject();
            action.addProperty("action", actionName);
            action.add("params", params);
            String echo = UUID.randomUUID().toString();
            action.addProperty("echo", echo);

            String payload = GsonStatic.toJson(action);

            CountDownLatch latch = new CountDownLatch(1);
            boolean[] success = {false};
            boolean[] received = {false};
            RuntimeException[] error = {null};

            HttpClient httpClient = HttpClient.newHttpClient();
            WebSocket.Builder builder = httpClient.newWebSocketBuilder();

            if (StrUtil.isNotBlank(oneBotAccessToken)) {
                builder.header("Authorization", getAuthorization(oneBotAccessToken));
            }

            CompletableFuture<WebSocket> future = builder.buildAsync(
                    URI.create(oneBotUrl),
                    new WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.sendText(payload, true);
                            webSocket.request(1);
                        }

                        @Override
                        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            buffer.append(data);
                            if (last) {
                                String responseText = buffer.toString();
                                try {
                                    for (JsonObject response : parseOneBotResponses(responseText)) {
                                        String respEcho = getString(response, "echo");
                                        if (echo.equals(respEcho)) {
                                            received[0] = true;
                                            assertOneBotResponse(response);
                                            success[0] = true;
                                            latch.countDown();
                                            break;
                                        }
                                        if (isOneBotFailure(response)) {
                                            received[0] = true;
                                            error[0] = new IllegalStateException(getOneBotErrorMessage(response));
                                            latch.countDown();
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    if (e instanceof RuntimeException runtimeException) {
                                        error[0] = runtimeException;
                                    } else {
                                        error[0] = new IllegalStateException("OneBot WebSocket 响应解析失败: " + responseText, e);
                                    }
                                    received[0] = true;
                                    latch.countDown();
                                } finally {
                                    buffer.setLength(0);
                                }
                                if (received[0]) {
                                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
                                }
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            if (!received[0]) {
                                error[0] = new IllegalStateException(StrFormatter.format(
                                        "OneBot WebSocket 在收到响应前关闭, status: {}, reason: {}",
                                        statusCode,
                                        reason
                                ));
                            }
                            latch.countDown();
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            OneBotNotification.log.error("OneBot WebSocket 异常", error);
                            thisCompleteError(error);
                        }

                        private void thisCompleteError(Throwable throwable) {
                            error[0] = new IllegalStateException("OneBot WebSocket 异常: " + throwable.getMessage(), throwable);
                            latch.countDown();
                        }
                    }
            );

            WebSocket webSocket = future.get(10, TimeUnit.SECONDS);
            boolean finished = latch.await(15, TimeUnit.SECONDS);

            if (!finished) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
                throw new IllegalStateException("OneBot WebSocket 等待响应超时");
            }

            if (error[0] != null) {
                throw error[0];
            }
            if (!success[0]) {
                throw new IllegalStateException("OneBot WebSocket 未收到成功响应");
            }

            return true;
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("OneBot WebSocket 通知异常: " + e.getMessage(), e);
        }
    }

    private static JsonObject buildParams(
            Ani ani,
            String text,
            String oneBotMessageType,
            String oneBotUserId,
            String oneBotGroupId,
            Boolean oneBotImage
    ) {
        JsonObject params = new JsonObject();
        if ("GROUP".equals(oneBotMessageType)) {
            params.addProperty("group_id", parseId(oneBotGroupId, "群号"));
        } else {
            params.addProperty("user_id", parseId(oneBotUserId, "用户 ID"));
        }
        params.add("message", buildMessage(ani, text, oneBotImage));
        return params;
    }

    private static JsonElement buildMessage(Ani ani, String text, Boolean oneBotImage) {
        if (!Boolean.TRUE.equals(oneBotImage)) {
            return new JsonPrimitive(text);
        }

        JsonArray message = buildImageMessage(ani, text);
        if (message.size() == 1) {
            return new JsonPrimitive(text);
        }
        return message;
    }

    private static JsonArray buildImageMessage(Ani ani, String text) {
        List<JsonObject> messageChain = new ArrayList<>();

        String imageBase64 = getImageBase64FromFile(ani);
        if (StrUtil.isNotBlank(imageBase64)) {
            JsonObject imageSegment = new JsonObject();
            imageSegment.addProperty("type", "image");
            JsonObject imageData = new JsonObject();
            imageData.addProperty("file", "base64://" + imageBase64);
            imageSegment.add("data", imageData);
            messageChain.add(imageSegment);
        }

        JsonObject textSegment = new JsonObject();
        textSegment.addProperty("type", "text");
        JsonObject textData = new JsonObject();
        textData.addProperty("text", text);
        textSegment.add("data", textData);
        messageChain.add(textSegment);

        JsonArray jsonArray = new JsonArray();
        messageChain.forEach(jsonArray::add);
        return jsonArray;
    }

    private static String getImageBase64FromFile(Ani ani) {
        if (ani == null || StrUtil.isBlank(ani.getCover())) {
            return "";
        }

        String filename = ani.getCover();
        File file = new File(filename);
        File configDir = ConfigUtil.getConfigDir();
        if (!file.exists()) {
            file = new File(configDir + "/files/" + filename);
        }
        if (!file.exists() || !file.isFile()) {
            return "";
        }

        try {
            return Base64.getEncoder().encodeToString(FileUtil.readBytes(file));
        } catch (Exception e) {
            log.warn("OneBot 读取封面失败: {}", file);
            return "";
        }
    }

    private static String getActionName(String oneBotMessageType) {
        if ("GROUP".equals(oneBotMessageType)) {
            return "send_group_msg";
        }
        return "send_private_msg";
    }

    private static long parseId(String id, String fieldName) {
        try {
            return Long.parseLong(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("OneBot " + fieldName + "必须是数字", e);
        }
    }

    private static String getAuthorization(String oneBotAccessToken) {
        return "Bearer " + oneBotAccessToken;
    }

    private static void assertOneBotResponse(String responseText) {
        if (StrUtil.isBlank(responseText)) {
            throw new IllegalStateException("OneBot 响应为空");
        }
        JsonObject response = GsonStatic.fromJson(responseText, JsonObject.class);
        assertOneBotResponse(response);
    }

    private static void assertOneBotResponse(JsonObject response) {
        String status = getString(response, "status");
        Integer retcode = getInt(response, "retcode");

        if ("failed".equals(status)) {
            throw new IllegalStateException(getOneBotErrorMessage(response));
        }
        if (retcode != null && retcode != 0) {
            throw new IllegalStateException(getOneBotErrorMessage(response));
        }
        if (StrUtil.isNotBlank(status) && !"ok".equals(status)) {
            throw new IllegalStateException(getOneBotErrorMessage(response));
        }
        if (StrUtil.isBlank(status) && retcode == null) {
            throw new IllegalStateException("OneBot 响应缺少 status/retcode");
        }
    }

    private static List<JsonObject> parseOneBotResponses(String responseText) {
        List<JsonObject> responses = new ArrayList<>();
        try {
            JsonStreamParser parser = new JsonStreamParser(responseText);
            while (parser.hasNext()) {
                JsonElement jsonElement = parser.next();
                if (jsonElement.isJsonObject()) {
                    responses.add(jsonElement.getAsJsonObject());
                }
            }
        } catch (Exception e) {
            log.warn("OneBot WebSocket 响应解析失败: {}", responseText);
            throw new IllegalStateException("OneBot WebSocket 响应解析失败", e);
        }
        if (responses.isEmpty()) {
            log.warn("OneBot WebSocket 响应不是 JSON 对象: {}", responseText);
            throw new IllegalStateException("OneBot WebSocket 响应不是 JSON 对象");
        }
        return responses;
    }

    private static boolean isOneBotFailure(JsonObject response) {
        String status = getString(response, "status");
        Integer retcode = getInt(response, "retcode");
        return "failed".equals(status) || (retcode != null && retcode != 0);
    }

    private static String getOneBotErrorMessage(JsonObject response) {
        String message = getResponseMessage(response);
        message = StrUtil.blankToDefault(message, "unknown error");

        Integer retcode = getInt(response, "retcode");
        String status = getString(response, "status");
        return StrFormatter.format("OneBot 发送失败, status: {}, retcode: {}, message: {}", status, retcode, message);
    }

    private static String getResponseMessage(String responseText) {
        if (StrUtil.isBlank(responseText)) {
            return "";
        }
        try {
            JsonElement jsonElement = GsonStatic.fromJson(responseText, JsonElement.class);
            if (!jsonElement.isJsonObject()) {
                return "";
            }
            return getResponseMessage(jsonElement.getAsJsonObject());
        } catch (Exception e) {
            log.warn("OneBot 响应解析失败: {}", responseText);
            return "";
        }
    }

    private static String getResponseMessage(JsonObject response) {
        String message = getString(response, "wording");
        message = StrUtil.blankToDefault(message, getString(response, "message"));
        return StrUtil.blankToDefault(message, getString(response, "msg"));
    }

    private static String getString(JsonObject response, String key) {
        if (!response.has(key) || response.get(key).isJsonNull()) {
            return "";
        }
        return response.get(key).getAsString();
    }

    private static Integer getInt(JsonObject response, String key) {
        if (!response.has(key) || response.get(key).isJsonNull()) {
            return null;
        }
        return response.get(key).getAsInt();
    }
}
