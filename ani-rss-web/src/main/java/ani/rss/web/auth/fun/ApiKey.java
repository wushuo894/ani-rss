package ani.rss.web.auth.fun;

import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;

import java.util.function.Function;

/**
 * api key 鉴权
 */
public class ApiKey implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
        Config config = ConfigUtil.CONFIG;
        String apiKey = config.getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            return false;
        }
        String s = StrUtil.blankToDefault(request.getParam("s"), request.getHeader("s"));
        return StrUtil.equals(apiKey, s);
    }
}
