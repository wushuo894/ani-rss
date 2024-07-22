package ani.rss.auth.fun;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Config;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;

import java.util.function.Function;

/**
 * api key 鉴权
 */
@Auth(type = AuthType.API_KEY)
public class ApiKey implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
        Config config = ConfigUtil.CONFIG;
        String apiKey = config.getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            String ip = AuthUtil.getIp();
            return Ipv4Util.isInnerIP(ip);
        }
        String s = StrUtil.blankToDefault(request.getParam("s"), request.getHeader("s"));
        return StrUtil.equals(apiKey, s);
    }
}
