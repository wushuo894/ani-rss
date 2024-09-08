package ani.rss.auth.util;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ServerUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.server.HttpServerRequest;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AuthUtil {
    private static String KEY;
    private static final MD5 MD5 = new MD5();
    private static final Gson GSON = new Gson();
    private static final Map<String, Function<HttpServerRequest, Boolean>> MAP = new HashMap<>();

    static {
        resetKey();
    }

    /**
     * 刷新密钥
     */
    public static synchronized void resetKey() {
        KEY = RandomUtil.randomString(32);
    }

    public static synchronized String getAuth(Login login) {
        login.setKey(KEY);
        return MD5.digestHex(GSON.toJson(login));
    }

    public static Login getLogin() {
        Config config = ConfigUtil.CONFIG;
        Login login = ObjectUtil.clone(config.getLogin());
        login.setIp(getIp());
        return login;
    }

    public static String getIp() {
        try {
            HttpServerRequest request = ServerUtil.REQUEST.get();
            HttpExchange httpExchange = (HttpExchange) ReflectUtil.getFieldValue(request, "httpExchange");
            return httpExchange.getRemoteAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "未知";
    }

    public static Boolean test(HttpServerRequest request, AuthType authType) {
        synchronized (MAP) {
            if (MAP.isEmpty()) {
                Map<String, Function<HttpServerRequest, Boolean>> map = ClassUtil.scanPackage("ani.rss.auth.fun")
                        .stream()
                        .collect(Collectors.toMap(c -> c.getAnnotation(Auth.class).type().name(), i -> (Function<HttpServerRequest, Boolean>) ReflectUtil.newInstance(i)));
                MAP.putAll(map);
            }
        }
        String name = authType.name();
        Function<HttpServerRequest, Boolean> function = MAP.get(name);
        if (Objects.isNull(function)) {
            return false;
        }
        return function.apply(request);
    }

}
