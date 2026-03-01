package ani.rss.auth.fun;

import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.function.Function;

/**
 * api key 鉴权
 */
public class ApiKey implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        Config config = ConfigUtil.CONFIG;
        String apiKey = config.getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            return false;
        }

        for (String key : List.of("api-key", "x-api-key", "s")) {
            String s = request.getHeader(key);
            if (StrUtil.isBlank(s)) {
                s = request.getParameter(key);
            }
            if (StrUtil.isBlank(s)) {
                continue;
            }
            return StrUtil.equals(apiKey, s);
        }

        return false;
    }
}
