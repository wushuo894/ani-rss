package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Result;
import ani.rss.util.LogUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

@Path("/logs")
public class LogsAction implements Action {
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        List<String> logs = LogUtil.getLogs();
        String json = gson.toJson(Result.success(logs));
        IoUtil.writeUtf8(res.getOut(), true, json);
    }
}
