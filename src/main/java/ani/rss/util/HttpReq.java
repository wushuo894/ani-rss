package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Objects;

@Slf4j
public class HttpReq {

    public static HttpRequest post(String url) {
        return post(url, false);
    }

    private static void config(HttpRequest req) {
        req.timeout(1000 * 20)
                .setFollowRedirects(true);

        String ua = "wushuo894/ani-rss (https://github.com/wushuo894/ani-rss)";

        req.header(Header.USER_AGENT, ua);
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

    public static String getUrl(HttpResponse response) {
        URL url = ((HttpConnection) ReflectUtil.getFieldValue(response, "httpConnection")).getUrl();
        return url.toString();
    }

    public static void assertStatus(HttpResponse response) {
        boolean ok = response.isOk();
        int status = response.getStatus();
        String url = getUrl(response);
        Assert.isTrue(ok, "url: {}, status: {}", url, status);
    }
}
