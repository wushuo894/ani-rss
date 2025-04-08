package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import ani.rss.util.BgmUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TmdbUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
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
        if (type.equals("search")) {
            String name = request.getParam("name");
            resultSuccess(BgmUtil.search(name));
            return;
        }

        if (type.equals("getTitle")) {
            Ani ani = getBody(Ani.class);
            TmdbUtil.Tmdb tmdb = ani.getTmdb();

            String title = ani.getTitle();

            BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);

            String nameCn = bgmInfo.getNameCn();
            String name = bgmInfo.getName();

            String bgmTitle = StrUtil.blankToDefault(nameCn, name);

            if (ReUtil.contains(StringEnum.YEAR_REG, title)) {
                bgmTitle = StrFormatter.format("{} ({})", bgmTitle, DateUtil.year(bgmInfo.getDate()));
            }

            Config config = ConfigUtil.CONFIG;

            if (config.getTmdbId()) {
                bgmTitle = StrFormatter.format("{} [tmdbid={}]", bgmTitle, tmdb.getId());
            }

            resultSuccess(bgmTitle);
        }

    }
}
