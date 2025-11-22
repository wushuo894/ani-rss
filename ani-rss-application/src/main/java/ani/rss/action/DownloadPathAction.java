package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * 获取下载位置
 */
@Auth
@Path("/downloadPath")
public class DownloadPathAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        String downloadPath = DownloadService.getDownloadPath(ani);

        boolean change = false;
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isPresent()) {
            Ani oldAni = ObjectUtil.clone(first.get());
            // 只在名称改变时移动
            oldAni.setSeason(ani.getSeason());
            String oldDownloadPath = DownloadService.getDownloadPath(oldAni);
            change = !downloadPath.equals(oldDownloadPath);
        }

        resultSuccess(Map.of(
                "change", change,
                "downloadPath", downloadPath
        ));
    }
}
