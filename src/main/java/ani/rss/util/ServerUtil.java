package ani.rss.util;

import ani.rss.action.BaseAction;
import ani.rss.action.RootAction;
import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.Result;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> request = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> response = new ThreadLocal<>();

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
            server.addAction(urlPath, new BaseAction() {
                private final Log log = Log.get(aClass);

                @Override
                public void doAction(HttpServerRequest req, HttpServerResponse res) {
                    try {
                        Auth auth = aClass.getAnnotation(Auth.class);
                        boolean isAuth = true;
                        if (Objects.nonNull(auth)) {
                            isAuth = auth.value();
                        }
                        if (isAuth) {
                            String authorization = req.getHeader("Authorization");
                            if (StrUtil.isBlank(authorization)) {
                                result(new Result<>().setCode(403));
                                return;
                            }
                            Config config = ConfigUtil.getCONFIG();
                            Login login = config.getLogin();
                            String username = login.getUsername();
                            String password = login.getPassword();
                            String s = MD5.create().digestHex(username + ":" + password);
                            if (!authorization.equals(s)) {
                                result(new Result<>().setCode(403));
                                return;
                            }
                        }
                        request.set(req);
                        response.set(res);
                        BaseAction baseAction = (BaseAction) action;
                        baseAction.doAction(req, res);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        res.setContentType("application/json; charset=utf-8");
                        String json = gson.toJson(Result.error().setMessage(message));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        if (!(e instanceof IllegalArgumentException)) {
                            log.error("{} {}", urlPath, e.getMessage());
                            log.debug(e);
                        }
                    }
                    request.remove();
                    response.remove();
                }
            });
        }
        return server;
    }
}
