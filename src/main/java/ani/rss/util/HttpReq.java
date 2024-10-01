package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Objects;

@Slf4j
public class HttpReq {

    public static HttpRequest post(String url) {
        return post(url, true);
    }

    private static void config(HttpRequest req) {
        req.timeout(6000)
                .setFollowRedirects(true);
    }

    public static HttpRequest post(String url, Boolean proxy) {
        HttpRequest req = HttpRequestPlus.post(url);
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
        HttpRequest req = HttpRequestPlus.get(url);
        config(req);
        if (proxy) {
            setProxy(req);
        }
        return req;
    }

    public static HttpRequest put(String url) {
        return put(url, false);
    }

    public static HttpRequest put(String url, Boolean proxy) {
        HttpRequest req = HttpRequestPlus.put(url);
        config(req);
        if (proxy) {
            setProxy(req);
        }
        return req;
    }

    public static HttpRequest delete(String url) {
        return delete(url, false);
    }

    public static HttpRequest delete(String url, Boolean proxy) {
        HttpRequest req = HttpRequestPlus.delete(url);
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
        return setProxy(req, ConfigUtil.CONFIG);
    }

    /**
     * 设置代理
     *
     * @param req
     * @param config
     * @return
     */
    public static HttpRequest setProxy(HttpRequest req, Config config) {
        String url = req.getUrl();
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

        String proxyUsername = config.getProxyUsername();
        String proxyPassword = config.getProxyPassword();
        try {
            req.setHttpProxy(proxyHost, proxyPort);
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            if (StrUtil.isAllNotBlank(proxyUsername, proxyPassword)) {
                                return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                            }
                            return null;
                        }
                    }
            );
            log.debug("使用代理 {}", url);
        } catch (Exception e) {
            log.error("设置代理出现问题 {}", url);
            log.error(e.getMessage(), e);
        }
        return req;
    }
}
