package ani.rss.auth.util;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.ServerUtil;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.util.*;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.server.HttpServerRequest;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AuthUtil {
    private static final MD5 MD5 = new MD5();
    private static final Gson GSON = new Gson();
    private static final Map<String, Function<HttpServerRequest, Boolean>> MAP = new HashMap<>();
    private static final FIFOCache<String, String> CACHE = CacheUtil.newFIFOCache(1);

    static {
        resetKey();
    }

    /**
     * 刷新有效时间
     */
    @Synchronized("CACHE")
    public static void resetTime() {
        String key = CACHE.get("key");
        if (StrUtil.isBlank(key)) {
            return;
        }
        Config config = ConfigUtil.CONFIG;
        Integer loginEffectiveHours = config.getLoginEffectiveHours();
        CACHE.put("key", key, TimeUnit.HOURS.toMillis(loginEffectiveHours));
    }

    /**
     * 刷新密钥
     */
    @Synchronized("CACHE")
    public static String resetKey() {
        String key = RandomUtil.randomString(128);
        Config config = ConfigUtil.CONFIG;
        Integer loginEffectiveHours = config.getLoginEffectiveHours();
        CACHE.put("key", key, TimeUnit.HOURS.toMillis(loginEffectiveHours));
        return key;
    }

    @Synchronized("CACHE")
    public static String getAuth(Login login) {
        String key = CACHE.get("key");
        if (StrUtil.isBlank(key)) {
            key = resetKey();
        }
        login.setKey(key);
        return MD5.digestHex(GSON.toJson(login)) + ":" + key;
    }

    public static Login getLogin() {
        Config config = ConfigUtil.CONFIG;
        Login login = ObjectUtil.clone(config.getLogin());
        if (config.getVerifyLoginIp()) {
            login.setIp(getIp());
        } else {
            login.setIp("");
        }
        return login;
    }

    public static String getIp() {
        try {
            HttpServerRequest request = ServerUtil.REQUEST.get();
            HttpExchange httpExchange = (HttpExchange) ReflectUtil.getFieldValue(request, "httpExchange");
            return httpExchange.getRemoteAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
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
