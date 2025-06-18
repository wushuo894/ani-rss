package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
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
public class WebHookNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String notificationTemplate = notificationConfig.getNotificationTemplate();
        notificationTemplate = notificationTemplate.replace("\n", "\\n");
        notificationTemplate = replaceNotificationTemplate(ani, notificationTemplate, text, notificationStatusEnum);

        String webHookMethod = notificationConfig.getWebHookMethod();
        String webHookUrl = notificationConfig.getWebHookUrl();
        String webHookBody = notificationConfig.getWebHookBody();

        webHookUrl = replaceNotificationTemplate(ani, webHookUrl, text, notificationStatusEnum);
        webHookBody = replaceNotificationTemplate(ani, webHookBody, text, notificationStatusEnum);

        if (StrUtil.isBlank(webHookUrl)) {
            log.warn("webhook url is blank");
            return false;
        }

        webHookUrl = webHookUrl.replace("${notification}", notificationTemplate);
        webHookBody = webHookBody.replace("${notification}", notificationTemplate);

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
