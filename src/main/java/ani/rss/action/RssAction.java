package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Result;
import ani.rss.util.AniUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

@Path("/rss")
public class RssAction implements Action {
    private final Log log = Log.get(RssAction.class);
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        if (!req.getMethod().equals("POST")) {
            return;
        }
        res.setContentType("application/json; charset=utf-8");
        try {
            String url = gson.fromJson(req.getBody(), Ani.class).getUrl();
            Ani ani = AniUtil.getAni(url);
            String json = gson.toJson(Result.success(ani));
            IoUtil.writeUtf8(res.getOut(), true, json);
        } catch (Exception e) {
            log.error(e);
            String json = gson.toJson(Result.error().setMessage(e.getMessage()));
            IoUtil.writeUtf8(res.getOut(), true, json);
        }
    }
}
