package ani.rss.auth.fun;

import ani.rss.commons.CacheUtils;
import ani.rss.entity.Config;
import ani.rss.util.basic.CidrRangeChecker;
import ani.rss.util.other.AuthUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class IpWhitelist implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        String ip = AuthUtil.getIp();
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
            if (!PatternPool.IPV4.matcher(ip).matches() && !PatternPool.IPV6.matcher(ip).matches()) {
                return false;
            }
            Boolean b = CacheUtils.get(key);
            if (Objects.nonNull(b)) {
                return b;
            }
            List<String> list = StrUtil.split(ipWhitelistStr, "\n", true, true);
            for (String string : list) {
                // 判断是否为 ipv4 或 ipv6
                if (PatternPool.IPV4.matcher(string).matches() || PatternPool.IPV6.matcher(string).matches()) {
                    if (string.equals(ip)) {
                        CacheUtils.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                // 通配符，如 192.168.*.1
                if (string.contains("*")) {
                    if (Ipv4Util.matches(string, ip)) {
                        CacheUtils.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                // X.X.X.X/X
                if (CidrRangeChecker.CIDR_PATTERN.matcher(string).matches()) {
                    if (CidrRangeChecker.isIpInRange(ip, string)) {
                        CacheUtils.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                // X.X.X.X-X.X.X.X
                if (isIpInRange(ip, string)) {
                    CacheUtils.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("ip白名单存在问题");
            log.error(e.getMessage(), e);
        }
        CacheUtils.put(key, Boolean.FALSE, TimeUnit.MINUTES.toMillis(10));
        return false;
    }

    /**
     * 判断ip是否在指定范围内
     *
     * @param ip ip地址
     * @param s  x.x.x.x-x.x.x.x
     * @return 判断结果
     */
    public static boolean isIpInRange(String ip, String s) {
        if (!s.contains("-")) {
            return false;
        }

        List<String> split = StrUtil.split(s, "-", true, true);
        if (split.size() != 2) {
            return false;
        }

        String startIp = split.get(0);
        String endIp = split.get(1);

        if (!PatternPool.IPV4.matcher(startIp).matches()) {
            return false;
        }

        if (!PatternPool.IPV4.matcher(endIp).matches()) {
            return false;
        }

        long ipLong = Ipv4Util.ipv4ToLong(ip);
        long startIpLong = Ipv4Util.ipv4ToLong(startIp);
        long endIpLong = Ipv4Util.ipv4ToLong(endIp);

        return ipLong >= startIpLong && ipLong <= endIpLong;
    }

}
