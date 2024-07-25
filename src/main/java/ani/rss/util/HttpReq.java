package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.log.Log;

import java.util.Objects;

public class HttpReq {
    private static final Log LOG = Log.get(HttpReq.class);

    public static HttpRequest post(String url) {
        return setProxy(HttpRequest.post(url));
    }

    public static HttpRequest get(String url) {
        return setProxy(HttpRequest.get(url));
    }


    /**
     * 设置代理
     *
     * @param req
     * @return
     */
    public static HttpRequest setProxy(HttpRequest req) {
        req.timeout(6000)
                .setFollowRedirects(true);
        String url = req.getUrl();
        Config config = ConfigUtil.getCONFIG();
        Boolean proxy = config.getProxy();
        if (!proxy) {
            LOG.debug("代理未开启 {}", url);
            return req;
        }
        String proxyHost = config.getProxyHost();
        Integer proxyPort = config.getProxyPort();
        if (StrUtil.isBlank(proxyHost) || Objects.isNull(proxyPort)) {
            LOG.debug("代理参数不全 {}", url);
            return req;
        }
        try {
            req.setHttpProxy(proxyHost, proxyPort);
            LOG.debug("使用代理 {}", url);
        } catch (Exception e) {
            LOG.error("设置代理出现问题 {}", url);
            LOG.error(e);
        }

        return req;
    }
}
