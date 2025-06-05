package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.JellyfinViews;
import ani.rss.util.JellyfinUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.List;

/**
 * 测试下载工具
 */
@Auth
@Path("/jellyfin")
public class JellyfinAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = getBody(Config.class);
        String type = request.getParam("type");

        if (type.equals("getViews")) {
            List<JellyfinViews> views = JellyfinUtil.getViews(config);
            resultSuccess(views);
            return;
        }

        if (type.equals("refresh")) {
            JellyfinUtil.refresh(config);
            resultSuccess();
        }

    }
}
