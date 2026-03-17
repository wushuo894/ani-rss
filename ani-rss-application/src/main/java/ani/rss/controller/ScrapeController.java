package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Ani;
import ani.rss.entity.web.Result;
import ani.rss.service.ScrapeService;
import cn.hutool.core.thread.ThreadUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScrapeController extends BaseController {

    @Resource
    private ScrapeService scrapeService;

    @Auth
    @Operation(summary = "刮削")
    @PostMapping("/scrape")
    public Result<Void> scrape(@RequestParam("force") Boolean force, @RequestBody Ani ani) {
        ThreadUtil.execute(() ->
                scrapeService.scrape(ani, force)
        );

        String title = ani.getTitle();

        return Result.success("已开始刮削 {}", title);
    }
}
