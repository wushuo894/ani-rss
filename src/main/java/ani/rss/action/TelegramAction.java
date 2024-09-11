package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.util.TelegramUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Map;

@Auth
@Path("/telegram")
public class TelegramAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = getBody(Config.class);
        String method = request.getParam("method");

        if ("test".equals(method)) {
            TelegramUtil.send(config,"test");
            resultSuccess();
            return;
        }

        if ("getUpdates".equals(method)) {
            Map<String, String> map = TelegramUtil.getUpdates(config);
            resultSuccess(map);
        }

    }
}
