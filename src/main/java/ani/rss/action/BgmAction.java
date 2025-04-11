package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.util.BgmUtil;
import ani.rss.util.TmdbUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * bgm
 */
@Auth
@Slf4j
@Path("/bgm")
public class BgmAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String type = request.getParam("type");
        switch (type) {
            case "search" -> {
                String name = request.getParam("name");
                resultSuccess(BgmUtil.search(name));
            }
            case "getAniBySubjectId" -> {
                String id = request.getParam("id");
                BgmInfo bgmInfo = BgmUtil.getBgmInfo(id, true);
                Ani ani = BgmUtil.toAni(bgmInfo, Ani.bulidAni());
                ani
                        .setCustomDownloadPath(true);
                resultSuccess(ani);
            }
            case "getTitle" -> {
                Ani ani = getBody(Ani.class);
                TmdbUtil.Tmdb tmdb = ani.getTmdb();
                BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
                resultSuccess(BgmUtil.getName(bgmInfo, tmdb));
            }
        }

    }
}
