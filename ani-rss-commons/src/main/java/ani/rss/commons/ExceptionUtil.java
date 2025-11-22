package ani.rss.commons;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

public class ExceptionUtil {
    public static final Map<String, String> messageMap = Map.of(
            "SocketTimeoutException", "网络连接超时",
            "UnknownHostException", "DNS 解析出错",
            "SSLHandshakeException", "SSL握手异常"
    );

    public static synchronized String getMessage(Exception e) {
        String message = e.getMessage();
        if (StrUtil.isBlank(message)) {
            return "";
        }
        for (Map.Entry<String, String> item : messageMap.entrySet()) {
            String key = item.getKey();
            String value = item.getValue();
            if (message.startsWith(key + ":")) {
                message = value;
            }
        }
        return message;
    }
}
