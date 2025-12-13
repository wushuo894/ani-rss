package ani.rss.action;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.TorrentUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.web.util.ServerUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Method;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 种子管理
 */
@Slf4j
@Auth
@Path("/torrent")
public class TorrentAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        if (Method.DELETE.name().equals(request.getMethod())) {
            del();
        }
    }


    /**
     * 删除缓存种子
     */
    public void del() {
        HttpServerRequest req = ServerUtil.REQUEST.get();
        String id = req.getParam("id");
        String infoHash = req.getParam("infoHash");
        Optional<Ani> first = AniUtil.ANI_LIST.stream().filter(ani -> id.equals(ani.getId()))
                .findFirst();
        if (first.isEmpty()) {
            resultErrorMsg("此订阅不存在");
            return;
        }

        List<String> infoHashList = StrUtil.split(infoHash, ",", true, true);

        Ani ani = first.get();
        File torrentDir = TorrentUtil.getTorrentDir(ani);
        File[] files = FileUtils.listFiles(torrentDir);
        for (File file : files) {
            String s = FileUtil.mainName(file);
            if (infoHashList.contains(s)) {
                log.info("删除种子 {}", file);
                FileUtil.del(file);
            }
        }
        resultSuccessMsg("删除完成");
    }
}
