package ani.rss.util;

import ani.rss.Main;
import ani.rss.action.BaseAction;
import ani.rss.action.RootAction;
import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Config;
import ani.rss.entity.Result;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static ani.rss.auth.util.AuthUtil.getIp;

@Slf4j
public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> RESPONSE = new ThreadLocal<>();
    public static String HOST = "";
    public static String HTTP_PORT = "7789";
    public static SimpleServer HTTP_SERVER;

    public static void start() {
        // 创建http/https服务
        createServer();

        // 添加过滤器
        addFilter(HTTP_SERVER);

        // 添加 action
        addAction(HTTP_SERVER);

        HTTP_SERVER.getRawServer().start();

        InetSocketAddress address = HTTP_SERVER.getAddress();
        String hostName = address.getHostName();
        int port = address.getPort();
        HTTP_PORT = String.valueOf(port);

        log.info("Http Server listen on [{}:{}]", hostName, port);

        for (String ip : NetUtil.localIpv4s()) {
            log.info("http://{}:{}", ip, port);
        }
    }

    /**
     * 创建http/https服务
     */
    public static void createServer() {
        Map<String, String> env = System.getenv();
        int i = Main.ARGS.indexOf("--port");
        if (i > -1) {
            HTTP_PORT = Main.ARGS.get(i + 1);
        }
        i = Main.ARGS.indexOf("--host");
        if (i > -1) {
            HOST = Main.ARGS.get(i + 1);
        }
        HTTP_PORT = env.getOrDefault("PORT", HTTP_PORT);
        HOST = env.getOrDefault("HOST", HOST);

        if (StrUtil.isBlank(HOST)) {
            HTTP_SERVER = new SimpleServer(Integer.parseInt(HTTP_PORT));
            return;
        }

        try {
            HTTP_SERVER = new SimpleServer(HOST, Integer.parseInt(HTTP_PORT));
            return;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        HTTP_SERVER = new SimpleServer(Integer.parseInt(HTTP_PORT));
    }

    public static void addFilter(SimpleServer server) {
        server.addFilter((req, res, chain) -> {
            REQUEST.set(req);
            RESPONSE.set(res);
            Config config = ConfigUtil.CONFIG;
            Boolean isInnerIP = config.getInnerIP();
            try {
                String ip = getIp();
                // 仅允许内网ip访问
                if (isInnerIP) {
                    if (!PatternPool.IPV4.matcher(ip).matches()) {
                        res.sendError(403, "已开启仅允许内网ip访问");
                        return;
                    }
                    if (!Ipv4Util.isInnerIP(ip)) {
                        res.sendError(403, "已开启仅允许内网ip访问");
                        return;
                    }
                }
                chain.doFilter(req.getHttpExchange());
            } finally {
                REQUEST.remove();
                RESPONSE.remove();
            }
        });
    }

    public static void addAction(SimpleServer server) {
        server.addAction("/", new RootAction());

        Set<Class<?>> classes = ClassUtil.scanPackage("ani.rss.action");
        for (Class<?> aClass : classes) {
            Path path = aClass.getAnnotation(Path.class);
            if (Objects.isNull(path)) {
                continue;
            }
            Object action = ReflectUtil.newInstanceIfPossible(aClass);
            String urlPath = "/api" + path.value();
            server.addAction(urlPath, new BaseAction() {
                private final Log log = Log.get(aClass);

                @Override
                public void doAction(HttpServerRequest req, HttpServerResponse res) {
                    try {
                        Auth auth = aClass.getAnnotation(Auth.class);
                        if (Objects.isNull(auth)) {
                            return;
                        }
                        Boolean test = AuthUtil.test(req, auth);
                        if (!test) {
                            BaseAction.staticResult(new Result<>().setCode(403).setMessage("登录状态失效"));
                            return;
                        }

                        BaseAction baseAction = (BaseAction) action;
                        baseAction.doAction(req, res);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        String json = GsonStatic.toJson(Result.error().setMessage(message));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        if (!(e instanceof IllegalArgumentException)) {
                            log.error("{} {}", urlPath, e.getMessage());
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            });
        }
    }

    public static void stop() {
        if (Objects.isNull(HTTP_SERVER)) {
            return;
        }
        try {
            HTTP_SERVER.getRawServer().stop(0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
