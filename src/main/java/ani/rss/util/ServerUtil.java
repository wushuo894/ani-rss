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

                // 判断是否为 ipv4
                if (PatternPool.IPV4.matcher(string).matches()) {
                    if (string.equals(ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }

                // 通配符，如 192.168.*.1
                if (string.contains("*")) {
                    if (Ipv4Util.matches(string, ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }

                // IP段，支持X.X.X.X-X.X.X.X或X.X.X.X/X
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
