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
    public Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        String messageTemplate = config.getMessageTemplate();
        messageTemplate = messageTemplate.replace("\n", "\\n");
        String message = replaceMessageTemplate(ani, messageTemplate, text);

        String webHookMethod = config.getWebHookMethod();
        String webHookUrl = config.getWebHookUrl();
        String webHookBody = config.getWebHookBody();

        webHookUrl = replaceMessageTemplate(ani, webHookUrl, text);
        webHookBody = replaceMessageTemplate(ani, webHookBody, text);

        if (StrUtil.isBlank(webHookUrl)) {
            log.warn("webhook url is blank");
            return false;
        }

        webHookUrl = webHookUrl.replace("${message}", message);
        webHookBody = webHookBody.replace("${message}", message);

        String image = "https://docs.wushuo.top/null.png";

        if (Objects.nonNull(ani) && StrUtil.isNotBlank(ani.getImage())) {
            image = ani.getImage();
        }

        webHookUrl = webHookUrl.replace("${image}", image);
        webHookBody = webHookBody.replace("${image}", image);

        HttpRequest httpRequest = HttpReq.get(webHookUrl, true)
                .method(Method.valueOf(webHookMethod));

        if (StrUtil.isNotBlank(webHookBody)) {
            httpRequest.body(webHookBody);
        }
        return httpRequest.thenFunction(HttpResponse::isOk);
    }
}
