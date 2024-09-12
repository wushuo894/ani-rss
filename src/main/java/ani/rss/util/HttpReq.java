package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class HttpReq {

    public static HttpRequest post(String url) {
        return post(url, true);
    }

    private static HttpRequest config(HttpRequest req) {
        return req.timeout(6000)
                .setFollowRedirects(true);
    }

    public static HttpRequest post(String url, Boolean proxy) {
        HttpRequest req = HttpRequest.post(url);
        config(req);
        if (proxy) {
            setProxy(req);
        }
        return req;
    }

    public static HttpRequest get(String url) {
        return get(url, false);
    }

    public static HttpRequest get(String url, Boolean proxy) {
        HttpRequest req = HttpRequest.get(url);
        config(req);
        if (proxy) {
            setProxy(req);
        }
        return req;
    }

    /**
     * 设置代理
     *
     * @param req
     * @return
     */
    public static HttpRequest setProxy(HttpRequest req) {
        String url = req.getUrl();
        Config config = ConfigUtil.CONFIG;
        Boolean proxy = config.getProxy();
        if (!proxy) {
            log.debug("代理未开启 {}", url);
            return req;
        }
        String proxyHost = config.getProxyHost();
        Integer proxyPort = config.getProxyPort();
        if (StrUtil.isBlank(proxyHost) || Objects.isNull(proxyPort)) {
            log.debug("代理参数不全 {}", url);
            return req;
        }
        try {
            req.setHttpProxy(proxyHost, proxyPort);
            log.debug("使用代理 {}", url);
        } catch (Exception e) {
            log.error("设置代理出现问题 {}", url);
            log.error(e.getMessage(), e);
        }

        return req;
    }
}
