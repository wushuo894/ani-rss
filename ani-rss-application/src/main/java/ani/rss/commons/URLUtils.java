package ani.rss.commons;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.net.URL;
import java.util.List;

public class URLUtils {
    /**
     * 自动添加http协议
     *
     * @param urlStr 链接
     * @return 处理后的链接
     */
    public static String getUrlStr(String urlStr) {
        if (StrUtil.isBlank(urlStr)) {
            return "";
        }

        if (!ReUtil.contains("^https?://", urlStr)) {
            urlStr = StrFormatter.format("http://{}", urlStr);
        }

        if (urlStr.endsWith("/")) {
            urlStr = urlStr.substring(0, urlStr.length() - 1);
        }

        return urlStr;
    }

    /**
     * 校验url安全性
     */
    public static void verify(String s) {
        Assert.notBlank(s, "URL 为空");

        String regex = "^https?://";

        Assert.isTrue(ReUtil.contains(regex, s), "错误的链接");

        URL url = URLUtil.url(s);

        String host = url.getHost();

        Assert.isFalse(
                List.of("127.0.0.1", "localhost").contains(host),
                "禁止访问回环网络"
        );

        if (PatternPool.IPV4.matcher(host).matches()) {
            Assert.isFalse(Ipv4Util.isInnerIP(host), "禁止访问内部网络");
        }
    }
}
