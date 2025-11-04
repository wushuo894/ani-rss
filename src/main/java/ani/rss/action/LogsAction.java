package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Log;
import ani.rss.util.basic.LogUtil;
import cn.hutool.http.Method;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 日志
 */
@Slf4j
@Auth
@Path("/logs")
public class LogsAction implements BaseAction {
    List<Log> LOG_LIST = LogUtil.LOG_LIST;

    @Override
    @Synchronized("LOG_LIST")
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        String method = req.getMethod();
        if (Method.DELETE.name().equals(method)) {
            LOG_LIST.clear();
            log.info("清理日志");
            resultSuccess();
            return;
        }
        resultSuccess(LOG_LIST);
    }
}
