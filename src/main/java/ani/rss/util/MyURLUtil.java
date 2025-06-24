package ani.rss.util;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

public class MyURLUtil {
    /**
     * 自动添加http协议
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

        if (urlStr.endsWith("/")) {
            urlStr = urlStr.substring(0, urlStr.length() - 1);
        }

        return urlStr;
    }
}
