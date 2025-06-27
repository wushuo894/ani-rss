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
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ani.rss.auth.util.AuthUtil.getIp;

@Slf4j
public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> RESPONSE = new ThreadLocal<>();
    public static String HOST = "";
    public static String PORT = "7789";
    public static SimpleServer server;

    public static void start() {
        Map<String, String> env = System.getenv();
        int i = Main.ARGS.indexOf("--port");
        if (i > -1) {
            PORT = Main.ARGS.get(i + 1);
        }
        i = Main.ARGS.indexOf("--host");
        if (i > -1) {
            HOST = Main.ARGS.get(i + 1);
        }
        PORT = env.getOrDefault("PORT", PORT);
        HOST = env.getOrDefault("HOST", HOST);

        if (StrUtil.isBlank(HOST)) {
            server = new SimpleServer(Integer.parseInt(PORT));
        } else {
            try {
                server = new SimpleServer(HOST, Integer.parseInt(PORT));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                server = new SimpleServer(Integer.parseInt(PORT));
            }
        }

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
        server.getRawServer().start();
        InetSocketAddress address = server.getAddress();
        log.info("Http Server listen on [{}:{}]", address.getHostName(), address.getPort());
        for (String ip : NetUtil.localIpv4s()) {
            log.info("http://{}:{}", ip, address.getPort());
        }
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
        Config config = ConfigUtil.CONFIG;
        String ipWhitelistStr = config.getIpWhitelistStr();
        Boolean ipWhitelist = config.getIpWhitelist();
        if (!ipWhitelist) {
            return false;
        }
        if (StrUtil.isBlank(ipWhitelistStr)) {
            return false;
        }
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        String key = "IpWhitelist:" + SecureUtil.md5(ipWhitelistStr) + ":" + ip;
        try {
            if (!PatternPool.IPV4.matcher(ip).matches()) {
                return false;
            }
            Boolean b = MyCacheUtil.get(key);
            if (Objects.nonNull(b)) {
                return b;
            }
            List<String> list = StrUtil.split(ipWhitelistStr, "\n", true, true);
            for (String string : list) {
                if (StrUtil.isBlank(string)) {
                    continue;
                }
                if (PatternPool.IPV4.matcher(string).matches()) {
                    if (string.equals(ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                if (string.contains("*")) {
                    if (Ipv4Util.matches(string, ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                List<String> ips = Ipv4Util.list(string, false);
                if (ips.contains(ip)) {
                    ips.clear();
                    MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                    return true;
                }
                ips.clear();
            }

        } catch (Exception e) {
            log.error("ip白名单存在问题");
            log.error(e.getMessage(), e);
        }
        MyCacheUtil.put(key, Boolean.FALSE, TimeUnit.MINUTES.toMillis(10));
        return false;
    }
}
