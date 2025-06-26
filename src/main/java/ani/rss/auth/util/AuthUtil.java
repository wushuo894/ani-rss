package ani.rss.auth.util;

import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.*;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 鉴权工具
 */
@Slf4j
public class AuthUtil {
    private static final Map<String, Function<HttpServerRequest, Boolean>> MAP = new HashMap<>();

    static {
        resetKey();
    }

    /**
     * 刷新有效时间
     */
    public static void resetTime() {
        String key = MyCacheUtil.get("auth_key");
        if (StrUtil.isBlank(key)) {
            return;
        }
        Config config = ConfigUtil.CONFIG;
        Integer loginEffectiveHours = config.getLoginEffectiveHours();
        MyCacheUtil.put("auth_key", key, TimeUnit.HOURS.toMillis(loginEffectiveHours));
    }

    /**
     * 刷新密钥
     */
    public static String resetKey() {
        Config config = ConfigUtil.CONFIG;

        // 登录有效时间/小时
        Integer loginEffectiveHours = config.getLoginEffectiveHours();
        Boolean multiLoginForbidden = config.getMultiLoginForbidden();

        String key = "123";

        if (multiLoginForbidden) {
            // 禁止多端登录
            key = RandomUtil.randomString(128);
        }
        MyCacheUtil.put("auth_key", key, TimeUnit.HOURS.toMillis(loginEffectiveHours));
        return key;
    }

    public static String getAuth(Login login) {
        String key = MyCacheUtil.get("auth_key");
        if (StrUtil.isBlank(key)) {
            key = resetKey();
        }
        login.setKey(key);
        return SecureUtil.sha256(GsonStatic.toJson(login));
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

    /**
     * 获取ip地址
     *
     * @return
     */
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

    /**
     * 鉴权检测
     *
     * @param request
     * @param authType
     * @return
     */
    public static Boolean test(HttpServerRequest request, AuthType authType) {
        Class<? extends Function<HttpServerRequest, Boolean>> clazz = authType.getClazz();
        String name = clazz.getName();
        Function<HttpServerRequest, Boolean> function = MAP.get(name);
        if (Objects.isNull(function)) {
            function = ReflectUtil.newInstance(clazz);
            MAP.put(name, function);
        }
        return function.apply(request);
    }

}
