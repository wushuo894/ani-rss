package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.ServerChanTypeEnum;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * ServerChan
 */
@Slf4j
public class ServerChanNotification implements BaseNotification {
    private static final String MARKDOWN_STRING = "# <message>\n\n![<image>](<image>)";

    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        ServerChanTypeEnum type = notificationConfig.getServerChanType();
        String sendKey = notificationConfig.getServerChanSendKey();
        String apiUrl = notificationConfig.getServerChan3ApiUrl();
        Boolean serverChanTitleAction = notificationConfig.getServerChanTitleAction();

        Boolean flag = checkParam(type, sendKey, apiUrl);
        if (!flag) {
            return false;
        }

        String title = "";
        String image = ani.getImage();


        title = truncateMessage(ani.getTitle(), serverChanTitleAction ? 10 : 15);
        if (serverChanTitleAction) {
            String action = notificationStatusEnum.getAction();
            title = StrFormatter.format("{}#{}", action, ani.getTitle());
        }

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        notificationTemplate = notificationTemplate.replace("\n", "\n\n");

        String serverChanUrl = "";
        String body = "";
        String desp = MARKDOWN_STRING
                .replace("<message>", notificationTemplate)
                .replace("<image>", image);

        if (type.equals(ServerChanTypeEnum.SERVER_CHAN)) {
            serverChanUrl = ServerChanTypeEnum.SERVER_CHAN.getUrl().replace("<sendKey>", sendKey);
            body = GsonStatic.toJson(Map.of(
                    "title", title,
                    "desp", desp
            ));
        }

        if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3)) {
            serverChanUrl = apiUrl;
            body = GsonStatic.toJson(Map.of(
                    "title", title,
                    "tags", "ani-rss",
                    "desp", desp
            ));
        }

        return HttpReq.post(serverChanUrl)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    private static Boolean checkParam(ServerChanTypeEnum type, String sendKey, String apiUrl) {
        if (type.equals(ServerChanTypeEnum.SERVER_CHAN)) {
            if (StrUtil.isBlank(sendKey)) {
                log.warn("sendKey 不能为空");
                return false;
            }
        }
        if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3)) {
            if (StrUtil.isBlank(apiUrl)) {
                log.warn("apiUrl 不能为空");
                return false;
            }
        }
        return true;
    }

    private String truncateMessage(String message, int maxLength) {
        if (StrUtil.isBlank(message)) {
            return "";
        }
        if (message.length() > maxLength) {
            return message.substring(0, maxLength - 3) + "...";
        }
        return message;
    }
}
