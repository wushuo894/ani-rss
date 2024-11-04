package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.other.Cron;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

/**
 * Trackers
 */
@Auth
@Path("/trackersUpdate")
public class TrackersUpdateAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = getBody(Config.class);
        Cron.updateTrackers(config);
        resultSuccessMsg("更新完成");
    }
}
