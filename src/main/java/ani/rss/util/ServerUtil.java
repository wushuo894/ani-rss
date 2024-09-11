package ani.rss.util;

import ani.rss.action.BaseAction;
import ani.rss.action.RootAction;
import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Result;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> RESPONSE = new ThreadLocal<>();
    public static String PORT = "7789";

    public static SimpleServer create(String... args) {
        Map<String, String> env = System.getenv();
        int i = Arrays.asList(args).indexOf("--port");

        if (i > -1) {
            PORT = args[i + 1];
        }

        PORT = env.getOrDefault("PORT", PORT);

        SimpleServer server = HttpUtil.createServer(Integer.parseInt(PORT));

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
                        REQUEST.set(req);
                        RESPONSE.set(res);
                        Auth auth = aClass.getAnnotation(Auth.class);
                        if (auth.value()) {
                            Boolean test = AuthUtil.test(req, auth.type());
                            if (!test) {
                                return;
                            }
                        }
                        BaseAction baseAction = (BaseAction) action;
                        baseAction.doAction(req, res);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        String json = gson.toJson(Result.error().setMessage(message));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        if (!(e instanceof IllegalArgumentException)) {
                            log.error("{} {}", urlPath, e.getMessage());
                            log.debug(e);
                        }
                    } finally {
                        REQUEST.remove();
                        RESPONSE.remove();
                    }
                }
            });
        }
        return server;
    }
}
