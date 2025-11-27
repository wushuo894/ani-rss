package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.entity.EmbyViews;
import ani.rss.entity.NotificationConfig;
import ani.rss.util.other.EmbyUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.List;

/**
 * Emby
 */
@Auth
@Path("/emby")
public class EmbyAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        NotificationConfig notificationConfig = getBody(NotificationConfig.class);
        String type = request.getParam("type");

        if (type.equals("getViews")) {
            List<EmbyViews> views = EmbyUtil.getViews(notificationConfig);
            resultSuccess(views);
            return;
        }

        if (type.equals("refresh")) {
            EmbyUtil.refresh(notificationConfig);
            resultSuccess();
        }

    }
}
