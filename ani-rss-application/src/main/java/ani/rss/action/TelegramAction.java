package ani.rss.action;

import ani.rss.entity.NotificationConfig;
import ani.rss.notification.TelegramNotification;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
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
        NotificationConfig notificationConfig = getBody(NotificationConfig.class);
        String method = request.getParam("method");

        if ("getUpdates".equals(method)) {
            Map<String, String> map = TelegramNotification.getUpdates(notificationConfig);
            resultSuccess(map);
        }
    }
}
