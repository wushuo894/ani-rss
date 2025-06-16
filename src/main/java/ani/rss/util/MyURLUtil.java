package ani.rss.util;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.net.URL;

public class MyURLUtil {
    /**
     * 自动添加http协议
     * 去除url的path
     *
     * @param urlStr
     * @return
     */
    public static String getUrlStr(String urlStr) {
        if (StrUtil.isBlank(urlStr)) {
            return "";
        }

        if (!ReUtil.contains("http(s*)://", urlStr)) {
            urlStr = StrFormatter.format("http://{}", urlStr);
        }

        URL url = URLUtil.url(urlStr);
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();

        urlStr = StrFormatter.format("{}://{}", protocol, host);

        if (port > 0) {
            urlStr += ":" + port;
        }
        return urlStr;
    }
}
