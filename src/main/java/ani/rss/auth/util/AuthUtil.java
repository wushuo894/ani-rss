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
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.server.HttpServerRequest;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthUtil {
    private static AES AES;
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
        String key = RandomUtil.randomString(32);
        AES = new AES(key.getBytes(StandardCharsets.UTF_8));
    }

    public static synchronized String getAuth(Login login) {
        return MD5.digestHex(AES.encryptBase64(GSON.toJson(login)));
    }

    public static Login getLogin() {
        Config config = ConfigUtil.CONFIG;
        Login login = ObjectUtil.clone(config.getLogin());
        login.setIp(getIp());
        return login;
    }

    public static String getIp() {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        HttpExchange httpExchange = (HttpExchange) ReflectUtil.getFieldValue(request, "httpExchange");
        return httpExchange.getRemoteAddress().getAddress().getHostAddress();
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
