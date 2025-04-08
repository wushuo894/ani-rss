package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.util.BgmUtil;
import ani.rss.util.TmdbUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
                List<File> downloadPath = TorrentUtil.getDownloadPath(ani);
                ani
                        .setCustomDownloadPath(true)
                        .setDownloadPath(FileUtil.getAbsolutePath(downloadPath.get(0).getAbsolutePath()));
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
