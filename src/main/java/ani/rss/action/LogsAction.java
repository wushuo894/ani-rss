package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Log;
import ani.rss.util.LogUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.util.List;

@Auth
@Path("/logs")
public class LogsAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        List<Log> logs = LogUtil.LOGS;
        resultSuccess(logs);
    }
}
