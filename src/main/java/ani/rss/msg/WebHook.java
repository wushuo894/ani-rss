package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.util.HttpReq;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * WebHook
 */
@Slf4j
public class WebHook implements Message {
    @Override
    public Boolean send(Config config, Ani ani, MessageEnum messageEnum, String text) {
        String webHookMethod = config.getWebHookMethod();
        String webHookUrl = config.getWebHookUrl();
        String webHookBody = config.getWebHookBody();

        if (StrUtil.isBlank(webHookUrl)) {
            log.warn("webhook url is blank");
            return false;
        }

        webHookUrl = webHookUrl.replace("${message}", text);
        webHookBody = webHookBody.replace("${message}", text);

        String image = "https://docs.wushuo.top/image/null.png";

        if (Objects.nonNull(ani) && StrUtil.isNotBlank(ani.getImage())) {
            image = ani.getImage();
        }

        webHookUrl = webHookUrl.replace("${image}", image);
        webHookBody = webHookBody.replace("${image}", image);

        HttpRequest httpRequest = HttpReq.get(webHookUrl)
                .method(Method.valueOf(webHookMethod));

        if (StrUtil.isNotBlank(webHookBody)) {
            httpRequest.body(webHookBody);
        }
        return httpRequest.thenFunction(HttpResponse::isOk);
    }
}
