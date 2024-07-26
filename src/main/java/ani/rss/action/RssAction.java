package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Result;
import ani.rss.util.AniUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Path("/rss")
public class RssAction implements Action {
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        if (!req.getMethod().equals("POST")) {
            return;
        }
        res.setContentType("application/json; charset=utf-8");
        String url = gson.fromJson(req.getBody(), Ani.class).getUrl();
        Assert.notBlank(url, "RSS地址 不能为空");
        if (!ReUtil.contains("http(s*)://", url)) {
            url = "https://" + url;
        }
        Ani ani = AniUtil.getAni(url);
        String json = gson.toJson(Result.success(ani));
        IoUtil.writeUtf8(res.getOut(), true, json);
    }
}
