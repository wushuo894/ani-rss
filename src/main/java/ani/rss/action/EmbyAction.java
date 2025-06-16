package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.EmbyViews;
import ani.rss.util.ConfigUtil;
import ani.rss.util.EmbyUtil;
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
        Config config = getBody(Config.class);
        ConfigUtil.format(config);
        String type = request.getParam("type");

        if (type.equals("getViews")) {
            List<EmbyViews> views = EmbyUtil.getViews(config);
            resultSuccess(views);
            return;
        }

        if (type.equals("refresh")) {
            EmbyUtil.refresh(config);
            resultSuccess();
        }

    }
}
