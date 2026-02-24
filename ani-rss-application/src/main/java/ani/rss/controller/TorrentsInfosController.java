package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Result;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.other.TorrentUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TorrentsInfosController {

    @Auth
    @Operation(summary = "下载列表")
    @PostMapping("/torrentsInfos")
    public Result<List<TorrentsInfo>> torrentsInfos() {
        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
        return Result.success(torrentsInfos);
    }

}
