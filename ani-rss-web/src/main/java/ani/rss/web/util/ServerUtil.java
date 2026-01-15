package ani.rss.web.util;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Config;
import ani.rss.entity.Global;
import ani.rss.entity.Result;
import ani.rss.exception.ResultException;
import ani.rss.util.other.ConfigUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.action.RootAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.log.Log;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static ani.rss.web.util.AuthUtil.getIp;

@Slf4j
public class ServerUtil {
    public static final ThreadLocal<HttpServerRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServerResponse> RESPONSE = new ThreadLocal<>();
    public static SimpleServer HTTP_SERVER;

    public static void start() {
        // 创建http/https服务
        createServer();

        // 添加过滤器
        addFilter(HTTP_SERVER);

        // 添加 action
        addAction(HTTP_SERVER);

        HttpServer rawServer = HTTP_SERVER.getRawServer();

        rawServer.start();

        InetSocketAddress address = rawServer.getAddress();
        String hostAddress = address.getAddress().getHostAddress();
        int port = address.getPort();
        Global.HTTP_PORT = String.valueOf(port);

        log.info("Http Server listen on [{}:{}]", hostAddress, port);

        for (String ip : NetUtil.localIpv4s()) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, port);
            if (NetUtil.isOpen(inetSocketAddress, 100)) {
                log.info("http://{}:{}", ip, port);
            }
        }

        RuntimeUtil.addShutdownHook(ServerUtil::stop);
    }

    /**
     * 创建http/https服务
     */
    public static void createServer() {
        Map<String, String> env = System.getenv();
        int i = Global.ARGS.indexOf("--port");
        if (i > -1) {
            Global.HTTP_PORT = Global.ARGS.get(i + 1);
        }
        i = Global.ARGS.indexOf("--host");
        if (i > -1) {
            Global.HOST = Global.ARGS.get(i + 1);
        }
        Global.HTTP_PORT = env.getOrDefault("PORT", Global.HTTP_PORT);
        Global.HOST = env.getOrDefault("HOST", Global.HOST);

        try {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(
                    Global.HOST,
                    Integer.parseInt(Global.HTTP_PORT)
            );
            HTTP_SERVER = new SimpleServer(inetSocketAddress);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
                        writeInnerIP();
                        return;
                    }
                    if (!Ipv4Util.isInnerIP(ip)) {
                        writeInnerIP();
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

    public static void writeInnerIP() {
        HttpServerResponse response = RESPONSE.get();
        String html = ResourceUtil.readUtf8Str("template.html");
        html = html.replace("${text}", "已开启仅允许内网ip访问");
        response.sendError(HttpStatus.HTTP_FORBIDDEN, html);
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
                        if (e instanceof ResultException resultException) {
                            result(resultException.getResult());
                            return;
                        }
                        String message = ExceptionUtils.getMessage(e);
                        resultErrorMsg(message);
                        if (e instanceof IllegalArgumentException) {
                            // 断言日志将不进行打印
                            return;
                        }
                        log.error("{} {}", urlPath, e.getMessage());
                        log.error(e.getMessage(), e);
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
            log.info("http server stop");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
