package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.ServerChanTypeEnum;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * ServerChan
 */
@Slf4j
public class ServerChan implements Message {
    private static final String MARKDOWN_STRING = "# <message>\n\n![<image>](<image>)";

    @Override
    public Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        text = replaceMessageTemplate(ani, config.getMessageTemplate(), text, messageEnum);
        text = text.replace("\n", "\n\n");
        String type = config.getServerChanType();
        String sendKey = config.getServerChanSendKey();
        String apiUrl = config.getServerChan3ApiUrl();
        Boolean serverChanTitleAction = config.getServerChanTitleAction();

        Boolean flag = checkParam(type, sendKey, apiUrl);
        if (!flag) {
            return false;
        }


        String image = "https://docs.wushuo.top/null.png";

        String title = text;
        if (Objects.nonNull(ani)) {
            if (StrUtil.isNotBlank(ani.getImage())) {
                image = ani.getImage();
            }
            title = truncateMessage(ani.getTitle(), serverChanTitleAction ? 10 : 15);
            if (serverChanTitleAction) {
                String action = messageEnum.getAction();
                title = StrFormatter.format("{}#{}", action, title);
            }
        }

        String serverChanUrl = "";
        String body = "";
        String desp = MARKDOWN_STRING.replace("<message>", text).replace("<image>", image);
        if (type.equals(ServerChanTypeEnum.SERVER_CHAN.getType())) {
            serverChanUrl = ServerChanTypeEnum.SERVER_CHAN.getUrl().replace("<sendKey>", sendKey);
            body = GsonStatic.toJson(Map.of(
                    "title", title,
                    "desp", desp
            ));
        } else if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3.getType())) {
            serverChanUrl = apiUrl;
            body = GsonStatic.toJson(Map.of(
                    "title", title,
                    "tags", "ass",
                    "desp", desp
            ));
        }

        return HttpReq.post(serverChanUrl, false)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    private static Boolean checkParam(String type, String sendKey, String apiUrl) {
        if (StrUtil.isBlank(type)) {
            log.warn("server酱类型不能为空");
            return false;
        }

        if (type.equals(ServerChanTypeEnum.SERVER_CHAN.getType())) {
            if (StrUtil.isBlank(sendKey)) {
                log.warn("sendKey 不能为空");
                return false;
            }
        } else if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3.getType())) {
            if (StrUtil.isBlank(apiUrl)) {
                log.warn("apiUrl 不能为空");
                return false;
            }
        } else {
            log.warn("无效的server酱类型");
            return false;
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
