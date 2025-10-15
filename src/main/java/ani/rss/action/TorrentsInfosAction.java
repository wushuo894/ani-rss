package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * 下载器任务列表
 */
@Slf4j
@Auth
@Path("/torrentsInfos")
public class TorrentsInfosAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
        resultSuccess(torrentsInfos);
    }
}
