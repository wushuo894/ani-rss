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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuthUtil {
    private static final AES AES;
    private static final MD5 MD5 = new MD5();
    private static final Gson GSON = new Gson();
    private static final Map<String, Class<Function<HttpServerRequest, Boolean>>> authTypeClassMap = new HashMap<>();

    static {
        // 每次重新启动之前的令牌都会失效
        String key = RandomUtil.randomString(32);
        AES = new AES(key.getBytes(StandardCharsets.UTF_8));
    }

    public static String getAuth(Login login) {
        return MD5.digestHex(AES.encryptBase64(GSON.toJson(login)));
    }

    public static Login getLogin() {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        Config config = ConfigUtil.CONFIG;
        Login login = ObjectUtil.clone(config.getLogin());
        login.setIp(request.getClientIP());
        return login;
    }

    public static Boolean test(HttpServerRequest request, AuthType authType) {
        synchronized (authTypeClassMap) {
            if (authTypeClassMap.isEmpty()) {
                Map<String, Class<Function<HttpServerRequest, Boolean>>> map = ClassUtil.scanPackage("ani.rss.auth.fun")
                        .stream()
                        .collect(Collectors.toMap(c -> c.getAnnotation(Auth.class).type().name(), i -> (Class<Function<HttpServerRequest, Boolean>>) i));
                authTypeClassMap.putAll(map);
            }
        }
        String name = authType.name();
        Class<Function<HttpServerRequest, Boolean>> functionClass = authTypeClassMap.get(name);
        if (Objects.isNull(functionClass)) {
            return false;
        }
        Function<HttpServerRequest, Boolean> httpRequestBooleanFunction = ReflectUtil.newInstance(functionClass);
        return httpRequestBooleanFunction.apply(request);
    }

}
