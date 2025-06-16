package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.msg.Telegram;
import ani.rss.util.ConfigUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Map;

/**
 * 电报
 */
@Auth
@Path("/telegram")
public class TelegramAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = getBody(Config.class);
        ConfigUtil.format(config);
        String method = request.getParam("method");

        if ("getUpdates".equals(method)) {
            Map<String, String> map = Telegram.getUpdates(config);
            resultSuccess(map);
        }

    }
}
