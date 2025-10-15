package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Log;
import ani.rss.util.basic.LogUtil;
import cn.hutool.http.Method;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 日志
 */
@Slf4j
@Auth
@Path("/logs")
public class LogsAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        String method = req.getMethod();
        List<Log> logs = LogUtil.LOGS;
        if (Method.DELETE.name().equals(method)) {
            logs.clear();
            log.info("清理日志");
            resultSuccess();
            return;
        }
        resultSuccess(logs);
    }
}
