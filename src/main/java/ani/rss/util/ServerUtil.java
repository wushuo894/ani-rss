package ani.rss.util;

import ani.rss.action.RootAction;
import ani.rss.annotation.Path;
import ani.rss.entity.Result;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ServerUtil {
    public static SimpleServer create() {
        Map<String, String> env = System.getenv();
        String port = env.getOrDefault("PORT", "7789");
        SimpleServer server = HttpUtil.createServer(Integer.parseInt(port));

        server.addAction("/", new RootAction());
        Set<Class<?>> classes = ClassUtil.scanPackage("ani.rss.action");
        for (Class<?> aClass : classes) {
            Path path = aClass.getAnnotation(Path.class);
            if (Objects.isNull(path)) {
                continue;
            }
            Object action = ReflectUtil.newInstanceIfPossible(aClass);
            String urlPath = "/api" + path.value();
            server.addAction(urlPath, new Action() {
                private final Log log = Log.get(aClass);
                private final Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .create();

                @Override
                public void doAction(HttpServerRequest req, HttpServerResponse res) {
                    try {
                        ((Action) action).doAction(req, res);
                    } catch (IllegalArgumentException e) {
                        res.setContentType("application/json; charset=utf-8");
                        String json = gson.toJson(Result.error().setMessage(e.getMessage()));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                    } catch (Exception e) {
                        log.error("{} {}", urlPath, e.getMessage());
                        log.debug(e);
                    }
                }
            });
        }
        return server;
    }
}
