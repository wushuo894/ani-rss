package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.TorrentUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 获取下载位置
 */
@Auth
@Path("/downloadPath")
public class DownloadPathAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        List<File> downloadPath = TorrentUtil.getDownloadPath(ani);
        String downloadPathStr = downloadPath.get(0).toString().replace("\\", "/");
        resultSuccess(downloadPathStr);
    }
}
