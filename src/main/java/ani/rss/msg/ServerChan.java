package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.ServerChanTypeEnum;
import ani.rss.util.HttpReq;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;

import java.util.Map;
import java.util.Objects;

/**
 * ServerChan
 */
@Slf4j
public class ServerChan implements Message {
    private static final String MARKDOWN_STRING = "# <message>\n\n![<image>](<image>)";

    @Override
    public Boolean send(Config config, Ani ani, MessageEnum messageEnum, String text) {
        String type = config.getServerChanType();
        String sendKey = config.getServerChanSendKey();
        if (StringUtil.isBlank(type) || StringUtil.isBlank(sendKey)) {
            log.warn("ServerChan 通知的参数不完整");
            return false;
        }
        if (type.equals(ServerChanTypeEnum.SERVER_CHAN.getType())) {
            if (!sendKey.startsWith(ServerChanTypeEnum.SERVER_CHAN.getSendkeyPrefix())) {
                log.warn("sendKey不匹配，请确认类型为{}", ServerChanTypeEnum.SERVER_CHAN.getName());
                return false;
            }
        }

        if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3.getType())) {
            if (!sendKey.startsWith(ServerChanTypeEnum.SERVER_CHAN_3.getSendkeyPrefix())) {
                log.warn("sendKey不匹配，请确认类型为{}", ServerChanTypeEnum.SERVER_CHAN_3.getName());
                return false;
            }
        }

        String image = "https://docs.wushuo.top/image/null.png";

        if (Objects.nonNull(ani) && StrUtil.isNotBlank(ani.getImage())) {
            image = ani.getImage();
        }
        String desp = MARKDOWN_STRING.replace("<message>", text).replace("<image>", image);

        String serverChanUrl = "";
        String body = "";
        if (type.equals(ServerChanTypeEnum.SERVER_CHAN.getType())) {
            serverChanUrl = ServerChanTypeEnum.SERVER_CHAN.getUrl().replace("<sendKey>", sendKey);
            body = gson.toJson(Map.of(
                    "title", text,
                    "desp", text
            ));
        } else if (type.equals(ServerChanTypeEnum.SERVER_CHAN_3.getType())) {
            serverChanUrl = ServerChanTypeEnum.SERVER_CHAN_3.getUrl().replace("<sendKey>", sendKey);
            body = gson.toJson(Map.of(
                    "title", text,
                    "tags", "ass",
                    "desp", desp
            ));
        }

        return HttpReq.post(serverChanUrl, false)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }
}
