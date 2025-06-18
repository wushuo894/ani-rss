package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.NotificationConfig;
import ani.rss.notification.TelegramNotification;
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
