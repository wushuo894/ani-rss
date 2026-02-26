package ani.rss.auth.fun;

import ani.rss.entity.Config;
import ani.rss.util.basic.CidrRangeChecker;
import ani.rss.util.other.AuthUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
        try {
            List<String> list = StrUtil.split(ipWhitelistStr, "\n", true, true);
            for (String string : list) {
                if (string.equals(ip)) {
                    return true;
                }
                // 非ipv4
                if (!PatternPool.IPV4.matcher(ip).matches()) {
                    continue;
                }
                // 通配符，如 192.168.*.1
                if (string.contains("*")) {
                    if (Ipv4Util.matches(string, ip)) {
                        return true;
                    }
                }
                // X.X.X.X/X
                if (CidrRangeChecker.CIDR_PATTERN.matcher(string).matches()) {
                    if (CidrRangeChecker.isIpInRange(ip, string)) {
                        return true;
                    }
                }
                // X.X.X.X-X.X.X.X
                if (isIpInRange(ip, string)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("ip白名单存在问题");
            log.error(e.getMessage(), e);
        }
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
