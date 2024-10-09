package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import ani.rss.util.ServerUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.Method;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

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
        Ani ani = first.get();
        File torrentDir = TorrentUtil.getTorrentDir(ani);
        File[] files = ObjectUtil.defaultIfNull(torrentDir.listFiles(), new File[]{});
        for (File file : files) {
            String s = FileUtil.mainName(file);
            if (infoHash.equals(s)) {
                log.info("删除种子 {}", file);
                FileUtil.del(file);
            }
        }
        resultSuccessMsg("删除完成");
    }
}
