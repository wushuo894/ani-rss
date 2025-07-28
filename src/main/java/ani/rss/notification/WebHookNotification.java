package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Base64;

/**
 * WebHook
 */
@Slf4j
public class WebHookNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String webHookMethod = notificationConfig.getWebHookMethod();
        String webHookUrl = notificationConfig.getWebHookUrl();
        String webHookBody = notificationConfig.getWebHookBody();

        webHookUrl = replaceNotificationTemplate(ani, webHookUrl, text, notificationStatusEnum);
        webHookBody = replaceNotificationTemplate(ani, webHookBody, text, notificationStatusEnum);

        if (StrUtil.isBlank(webHookUrl)) {
            log.warn("webhook url is blank");
            return false;
        }

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        notificationTemplate = notificationTemplate.replace("\n", "\\n");

        webHookUrl = webHookUrl.replace("${message}", notificationTemplate);
        webHookUrl = webHookUrl.replace("${notification}", notificationTemplate);

        webHookBody = webHookBody.replace("${message}", notificationTemplate);
        webHookBody = webHookBody.replace("${notification}", notificationTemplate);

        String image = "https://docs.wushuo.top/null.png";

        image = Opt.ofNullable(ani)
                .map(Ani::getImage)
                .filter(StrUtil::isNotBlank)
                .orElse(image);

        webHookUrl = webHookUrl.replace("${image}", image);
        webHookBody = webHookBody.replace("${image}", image);

        if (StrUtil.contains(webHookBody, "${imageBase64}")) {
            String imageBase64 = getImageBase64FromFile(ani);

            webHookUrl = webHookUrl.replace("${imageBase64}", imageBase64);
            webHookBody = webHookBody.replace("${imageBase64}", imageBase64);
        }

        HttpRequest httpRequest = HttpReq.get(webHookUrl, true)
                .method(Method.valueOf(webHookMethod));

        if (StrUtil.isNotBlank(webHookBody)) {
            httpRequest.body(webHookBody);
        }
        return httpRequest.thenFunction(HttpResponse::isOk);
    }

    private static String getImageBase64FromFile(Ani ani) {
        // 1*1 pixel gif
        String imageBase64 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

        imageBase64 = Opt.ofNullable(ani)
                .map(a -> {
                    String filename = a.getCover();
                    File file = new File(filename);
                    File configDir = ConfigUtil.getConfigDir();
                    if (!file.exists()) {
                        file = new File(configDir + "/files/" + filename);
                    }
                    if (!file.exists()) {
                        return null;
                    }
                    return Base64.getEncoder().encodeToString(FileUtil.readBytes(file));
                }).filter(StrUtil::isNotBlank)
                .orElse(imageBase64);
        return imageBase64;
    }
}
