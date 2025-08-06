package ani.rss.auth.fun;

import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Config;
import ani.rss.util.CidrRangeChecker;
import ani.rss.util.ConfigUtil;
import ani.rss.util.MyCacheUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.server.HttpServerRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
public class IpWhitelist implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
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
            if (!PatternPool.IPV4.matcher(ip).matches()) {
                return false;
            }
            Boolean b = MyCacheUtil.get(key);
            if (Objects.nonNull(b)) {
                return b;
            }
            List<String> list = StrUtil.split(ipWhitelistStr, "\n", true, true);
            for (String string : list) {
                // 判断是否为 ipv4
                if (PatternPool.IPV4.matcher(string).matches()) {
                    if (string.equals(ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                // 通配符，如 192.168.*.1
                if (string.contains("*")) {
                    if (Ipv4Util.matches(string, ip)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
                // IP段，支持X.X.X.X-X.X.X.X或X.X.X.X/X
                if (CidrRangeChecker.CIDR_PATTERN.matcher(string).matches()) {
                    if (CidrRangeChecker.isIpInRange(ip, string)) {
                        MyCacheUtil.put(key, Boolean.TRUE, TimeUnit.MINUTES.toMillis(10));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("ip白名单存在问题");
            log.error(e.getMessage(), e);
        }
        MyCacheUtil.put(key, Boolean.FALSE, TimeUnit.MINUTES.toMillis(10));
        return false;
    }
}
