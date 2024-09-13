package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.HttpReq;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;

public class WebHook implements Message {
    @Override
    public Boolean send(Config config, Ani ani, String text) {
        String webHookMethod = config.getWebHookMethod();
        String webHookUrl = config.getWebHookUrl();
        String webHookBody = config.getWebHookBody();

        if (StrUtil.isBlank(webHookUrl)) {
            return false;
        }

        webHookUrl = webHookUrl.replace("${message}", text);
        webHookBody = webHookBody.replace("${message}", text);

        HttpRequest httpRequest = HttpReq.get(webHookUrl)
                .method(Method.valueOf(webHookMethod));

        if (StrUtil.isNotBlank(webHookBody)) {
            httpRequest.body(webHookBody);
        }
        return httpRequest.thenFunction(HttpResponse::isOk);
    }
}
