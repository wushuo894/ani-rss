package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Mikan;
import ani.rss.util.MikanUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Path("/mikan")
public class MikanAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String text = request.getParam("text");
        Mikan.Season season = getBody(Mikan.Season.class);
        resultSuccess(MikanUtil.list(text, season));
    }
}
