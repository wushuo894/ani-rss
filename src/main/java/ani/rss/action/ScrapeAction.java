package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.service.ScrapeService;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 刮削
 */
@Auth
@Slf4j
@Path("/scrape")
public class ScrapeAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);

        String force = request.getParam("force");

        ThreadUtil.execute(() ->
                ScrapeService.scrape(ani, Boolean.parseBoolean(force))
        );

        String title = ani.getTitle();

        resultSuccessMsg("已开始刮削 {}", title);
    }
}
