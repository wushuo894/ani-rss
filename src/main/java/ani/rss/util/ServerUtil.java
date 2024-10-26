package ani.rss.util;

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
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static ani.rss.auth.util.AuthUtil.getIp;

@Slf4j
public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> RESPONSE = new ThreadLocal<>();
    public static String PORT = "7789";
    public static SimpleServer server;

    public static SimpleServer create(String... args) {
        Map<String, String> env = System.getenv();
        int i = Arrays.asList(args).indexOf("--port");

        if (i > -1) {
            PORT = args[i + 1];
        }

        PORT = env.getOrDefault("PORT", PORT);

        server = HttpUtil.createServer(Integer.parseInt(PORT));

        server.addFilter((req, res, chain) -> {
            REQUEST.set(req);
            RESPONSE.set(res);
            Config config = ConfigUtil.CONFIG;
            Boolean isInnerIP = config.getInnerIP();
            try {
                String ip = getIp();
                if (isInnerIP) {
                    if (!PatternPool.IPV4.matcher(ip).matches()) {
                        res.send404("404 Not Found");
                        return;
                    }
                    if (!Ipv4Util.isInnerIP(ip)) {
                        res.send404("404 Not Found");
                        return;
                    }
                }
                chain.doFilter(req.getHttpExchange());
            } finally {
                REQUEST.remove();
                RESPONSE.remove();
            }
        });
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
                        if (auth.value() && !isIpWhitelist(getIp())) {
                            Boolean test = AuthUtil.test(req, auth.type());
                            if (!test) {
                                return;
                            }
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
        return server;
    }

    public static void stop() {
        if (Objects.isNull(server)) {
            return;
        }
        try {
            server.getRawServer().stop(0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static synchronized Boolean isIpWhitelist(String ip) {
        try {
            Config config = ConfigUtil.CONFIG;
            String ipWhitelistStr = config.getIpWhitelistStr();
            Boolean ipWhitelist = config.getIpWhitelist();
            if (!ipWhitelist) {
                return false;
            }
            if (StrUtil.isBlank(ipWhitelistStr)) {
                return false;
            }
            if (!PatternPool.IPV4.matcher(ip).matches()) {
                return false;
            }
            List<String> list = StrUtil.split(ipWhitelistStr, "\n", true, true);
            for (String string : list) {
                if (StrUtil.isBlank(string)) {
                    continue;
                }
                if (ip.equals(string)) {
                    return true;
                }
                if (Ipv4Util.list(string, false).contains(ip)) {
                    return true;
                }
                if (Ipv4Util.matches(string, ip)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("ip白名单存在问题");
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
