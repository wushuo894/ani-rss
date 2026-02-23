package ani.rss.util.basic;

import ani.rss.commons.CacheUtils;
import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.cookie.GlobalCookieManager;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpReq {


    public static final CookieManager COOKIE_MANAGER;

    static {
        COOKIE_MANAGER = new CookieManager();
        COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    private static void config(HttpRequest req) {
        GlobalCookieManager.setCookieManager(COOKIE_MANAGER);

        req.timeout(1000 * 20)
                .setFollowRedirects(true);

        String ua = "wushuo894/ani-rss (https://github.com/wushuo894/ani-rss)";

        req.header(Header.USER_AGENT, ua);
    }

    public static HttpRequest post(String url) {
        HttpRequest req = HttpRequestPlus.post(url);
        config(req);
        setProxy(req);
        return req;
    }

    public static HttpRequest get(String url) {
        HttpRequest req = HttpRequestPlus.get(url);
        config(req);
        setProxy(req);
        return req;
    }

    public static HttpRequest put(String url) {
        HttpRequest req = HttpRequestPlus.put(url);
        config(req);
        setProxy(req);
        return req;
    }

    public static HttpRequest delete(String url) {
        HttpRequest req = HttpRequestPlus.delete(url);
        config(req);
        setProxy(req);
        return req;
    }

    /**
     * 设置代理
     *
     * @param req
     * @return
     */
    public static void setProxy(HttpRequest req) {
        setProxy(req, ConfigUtil.CONFIG);
    }

    /**
     * 设置代理
     *
     * @param req
     * @param config
     * @return
     */
    public static void setProxy(HttpRequest req, Config config) {
        String url = req.getUrl();
        Boolean proxy = config.getProxy();
        if (!proxy) {
            log.debug("代理未开启 {}", url);
            return;
        }

        if (!isProxy(url)) {
            // 不进行代理
            return;
        }

        String proxyHost = config.getProxyHost();
        Integer proxyPort = config.getProxyPort();
        if (StrUtil.isBlank(proxyHost) || Objects.isNull(proxyPort)) {
            log.debug("代理参数不全 {}", url);
            return;
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

    /**
     * 是否代理
     *
     * @param url
     * @return
     */
    public static Boolean isProxy(String url) {
        String host = URLUtil.url(url).getHost();

        Config config = ConfigUtil.CONFIG;
        String proxyList = config.getProxyList();

        String key = StrFormatter.format("proxyList:{}", SecureUtil.md5(proxyList));

        List<String> split = CacheUtils.get(key);

        if (Objects.isNull(split)) {
            split = StrUtil.split(proxyList, "\n", true, true);
            CacheUtils.put(key, split, TimeUnit.MINUTES.toMillis(10));
        }

        if (split.isEmpty()) {
            return false;
        }

        if (split.contains(host)) {
            return true;
        }

        for (String s : split) {
            if (host.endsWith("." + s)) {
                return true;
            }
        }
        return false;
    }

}
