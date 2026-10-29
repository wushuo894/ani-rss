package ani.rss.util.other;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.web.ContentType;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QQ 开放平台公共请求工具
 */
public class QqUtil {

    public static final String TOKEN_URL = "https://bots.qq.com/app/getAppAccessToken";
    public static final String GATEWAY_URL = "https://api.sgroup.qq.com/gateway";
    public static final String OPEN_API = "https://api.sgroup.qq.com";

    private static final Map<String, TokenCache> TOKEN_CACHE = new ConcurrentHashMap<>();

    private record TokenCache(String token, long expireAt) {
    }

    /**
     * 获取 access_token，带缓存（提前 60 秒刷新）
     *
     * @param appId        QQ 开放平台 AppID
     * @param clientSecret QQ 开放平台 AppSecret
     * @return access_token
     */
    public static String getAccessToken(String appId, String clientSecret) {
        String key = appId + ":" + SecureUtil.md5(clientSecret);
        TokenCache cache = TOKEN_CACHE.get(key);
        if (cache != null && System.currentTimeMillis() < cache.expireAt()) {
            return cache.token();
        }

        synchronized (TOKEN_CACHE) {
            cache = TOKEN_CACHE.get(key);
            if (cache != null && System.currentTimeMillis() < cache.expireAt()) {
                return cache.token();
            }

            JsonObject body = new JsonObject();
            body.addProperty("appId", appId);
            body.addProperty("clientSecret", clientSecret);

            HttpResponse response = HttpReq.post(TOKEN_URL)
                    .contentType(ContentType.JSON)
                    .body(body.toString())
                    .execute();
            assertOk(response, "获取 QQ access_token");

            JsonObject json = GsonStatic.fromJson(response.body(), JsonObject.class);
            String token = json.get("access_token").getAsString();

            int expiresIn = 60;
            if (json.has("expires_in")) {
                try {
                    expiresIn = json.get("expires_in").getAsInt();
                } catch (Exception ignored) {
                }
            }

            long expireAt = System.currentTimeMillis() + Math.max(expiresIn - 60, 30) * 1000L;
            TOKEN_CACHE.put(key, new TokenCache(token, expireAt));
            return token;
        }
    }

    /**
     * 获取 WebSocket 网关地址
     *
     * @param token access_token
     * @return 网关地址
     */
    public static String getGatewayUrl(String token) {
        HttpResponse response = HttpReq.get(GATEWAY_URL)
                .header("Authorization", "QQBot " + token)
                .execute();
        assertOk(response, "获取 QQ 网关地址");
        return GsonStatic.fromJson(response.body(), JsonObject.class).get("url").getAsString();
    }

    /**
     * 校验 QQ 接口响应，失败时携带服务端返回信息抛出异常
     *
     * @param response 响应
     * @param action   操作描述
     */
    public static void assertOk(HttpResponse response, String action) {
        if (response.isOk()) {
            return;
        }
        String body = StrUtil.blankToDefault(response.body(), "");
        throw new RuntimeException(StrFormatter.format("{}失败 ({})：{}", action, response.getStatus(), body));
    }
}
