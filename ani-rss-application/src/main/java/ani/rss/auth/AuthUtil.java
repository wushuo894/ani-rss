package ani.rss.auth;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.cache.CacheUtils;
import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Config;
import ani.rss.entity.Global;
import ani.rss.entity.web.Result;
import ani.rss.entity.web.ResultCode;
import ani.rss.exception.ResultException;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 鉴权工具
 */
@Slf4j
public class AuthUtil {
    private static final Config CONFIG = ConfigUtil.CONFIG;
    private static final Map<String, Function<HttpServletRequest, Boolean>> MAP = new HashMap<>();
    private static String SESSION_ID = "-";

    static {
        resetSessionId();
    }

    /**
     * 刷新 SessionId
     */
    public static String resetSessionId() {
        Boolean multiLoginForbidden = CONFIG.getMultiLoginForbidden();
        if (multiLoginForbidden) {
            SESSION_ID = UUID.randomUUID().toString();
        } else {
            SESSION_ID = "-";
        }
        return SESSION_ID;
    }

    /**
     * 生成 Token
     *
     * @return Token
     */
    public static String getToken() {
        String sessionId = resetSessionId();

        String tokenId = CONFIG.getTokenId();

        int loginEffectiveHours = CONFIG.getLoginEffectiveHours();
        long expireTime = 0;
        if (loginEffectiveHours > 0) {
            expireTime = DateUtil.offsetHour(new Date(), loginEffectiveHours).getTime();
        }

        String ip = getIp();

        Map<String, Object> map = Map.of(
                "sessionId", sessionId,
                "expireTime", expireTime,
                "tokenId", tokenId,
                "ip", ip
        );

        String jwtKey = CONFIG.getJwtKey();
        return JWTUtil.createToken(map, Base64.decode(jwtKey));
    }

    /**
     * 校验 Token
     *
     * @param token Token
     * @return 是否校验通过
     */
    public static Boolean verify(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }

        try {
            String jwtKey = CONFIG.getJwtKey();
            if (!JWTUtil.verify(token, Base64.decode(jwtKey))) {
                // Token 校验未通过
                return false;
            }

            JWT jwt = JWTUtil.parseToken(token);
            JSONObject payloads = jwt.getPayloads();

            boolean verifyLoginIp = CONFIG.getVerifyLoginIp();
            if (verifyLoginIp) {
                // 校验登陆 ip
                String ip = payloads.getStr("ip");
                String currentIp = getIp();
                if (!ip.equals(currentIp)) {
                    return false;
                }
            }

            String tokenId = payloads.getStr("tokenId");
            if (!tokenId.equals(CONFIG.getTokenId())) {
                // TokenVersion 不匹配, 可能已经修改密码
                return false;
            }

            long expireTime = payloads.getLong("expireTime");
            if (expireTime > 0 && expireTime < new Date().getTime()) {
                // Token 已过期
                return false;
            }

            // 会话ID, 多设备登陆可以顶掉
            String sessionId = payloads.getStr("sessionId");
            return SESSION_ID.equals(sessionId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取ip地址
     *
     * @return IP地址
     */
    public static String getIp() {
        try {
            List<String> reverseProxyTrustIpList = CONFIG.getReverseProxyTrustIpList();
            Boolean reverseProxyTrustIpListEnabled = CONFIG.getReverseProxyTrustIpListEnabled();

            HttpServletRequest request = Global.REQUEST.get();
            String ip = request.getRemoteAddr();
            if (!reverseProxyTrustIpListEnabled) {
                // 未启用 受信任的反向代理IP
                return ip;
            }

            if (!reverseProxyTrustIpList.contains(ip)) {
                // 不在名单中, 受信任的反向代理IP
                return ip;
            }

            // https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Reference/Headers/X-Forwarded-For
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StrUtil.isNotBlank(forwardedFor)) {
                // 获取第一个IP
                return NetUtil.getMultistageReverseProxyIp(forwardedFor);
            }

            return ip;
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
        }
        return "未知";
    }

    /**
     * 鉴权检测
     *
     * @param request HttpServletRequest
     * @param auth    鉴权注解
     * @return 是否通过鉴权
     */
    public static Boolean test(HttpServletRequest request, Auth auth) {
        limitLoginAttempts(false);
        if (!auth.value()) {
            // 不进行校验
            return true;
        }
        for (AuthType type : auth.type()) {
            Boolean test = test(request, type);
            if (test) {
                return true;
            }
        }
        limitLoginAttempts(true);
        return false;
    }

    /**
     * 鉴权检测
     *
     * @param request  HttpServletRequest
     * @param authType 鉴权类型
     * @return 是否通过鉴权
     */
    public static Boolean test(HttpServletRequest request, AuthType authType) {
        Class<? extends Function<HttpServletRequest, Boolean>> clazz = authType.getClazz();
        String name = clazz.getName();
        Function<HttpServletRequest, Boolean> function = MAP.get(name);
        if (Objects.isNull(function)) {
            function = ReflectUtil.newInstance(clazz);
            MAP.put(name, function);
        }
        return function.apply(request);
    }

    /**
     * 限制尝试次数
     *
     * @param isAdd 累加计数
     */
    public static void limitLoginAttempts(Boolean isAdd) {
        boolean limitLoginAttempts = CONFIG.getLimitLoginAttempts();
        if (!limitLoginAttempts) {
            return;
        }
        String ip = AuthUtil.getIp();
        String key = "LimitLoginAttempts#" + ip;

        // 1 天内将不再允许尝试
        long timeout = TimeUnit.DAYS.toMillis(1);

        if (!CacheUtils.containsKey(key)) {
            if (isAdd) {
                CacheUtils.put(key, new AtomicInteger(1), timeout);
            }
            return;
        }

        AtomicInteger countAtomicInteger = CacheUtils.get(key);
        int count = countAtomicInteger.getAndAdd(isAdd ? 1 : 0);

        // 失败时 时间将重新计时
        CacheUtils.put(key, countAtomicInteger, timeout);

        // 失败 30 次
        if (count < 30) {
            return;
        }

        log.debug("失败次数过多, 已限制登录 {}", ip);
        Result<Void> result = new Result<Void>()
                .setMessage(StrFormatter.format("失败次数过多, 已限制登录 {}", ip))
                .setCode(ResultCode.HTTP_FORBIDDEN);
        throw new ResultException(result);
    }

}
