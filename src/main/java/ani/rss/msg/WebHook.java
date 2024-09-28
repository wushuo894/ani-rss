package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.BgmUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;

import java.util.Objects;

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

        if (Objects.nonNull(ani) &&
                (webHookBody.contains("${image}") || webHookUrl.contains("${image}"))) {
            String image = ani.getImage();
            if (StrUtil.isBlank(image)) {
                image = BgmUtil.getBgmInfo(ani).getImage();
            }
            if (StrUtil.isNotBlank(image)) {
                webHookUrl = webHookUrl.replace("${image}", image);
                webHookBody = webHookBody.replace("${image}", image);
            }
        }

        HttpRequest httpRequest = HttpReq.get(webHookUrl)
                .method(Method.valueOf(webHookMethod));

        if (StrUtil.isNotBlank(webHookBody)) {
            httpRequest.body(webHookBody);
        }
        return httpRequest.thenFunction(HttpResponse::isOk);
    }
}
