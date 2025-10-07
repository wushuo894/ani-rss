package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.enums.StringEnum;
import ani.rss.util.AniUtil;
import ani.rss.util.FilePathUtil;
import ani.rss.util.PlaylistUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 视频列表
 */
@Auth
@Path("/playlist")
public class PlaylistAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        String url = ani.getUrl();
        Optional<Ani> first = AniUtil.ANI_LIST
                .stream()
                .filter(it -> url.equals(it.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            resultError();
            return;
        }
        ani = first.get();

        File downloadPath = TorrentUtil.getDownloadPath(ani);
        List<PlayItem> collect = PlaylistUtil.getPlayItem(downloadPath);
        collect = CollUtil.distinct(collect, PlayItem::getTitle, false);
        collect = CollUtil.sort(collect, Comparator.comparingDouble(it -> Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, it.getTitle(), 2))));
        resultSuccess(collect);
    }

}
