package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Ani;
import ani.rss.entity.web.Result;
import ani.rss.service.ScrapeService;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
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

    @Auth
    @Operation(summary = "批量刮削")
    @PostMapping("/batchScrape")
    public Result<Void> scrape(@RequestParam("force") Boolean force, @RequestBody List<String> ids) {
        Assert.notEmpty(ids, "未选择订阅");

        List<Ani> anis = AniUtil.ANI_LIST
                .stream()
                .filter(ani -> ids.contains(ani.getId()))
                .toList();

        ThreadUtil.execute(() -> {
            log.info("开始批量刮削...");
            for (Ani ani : anis) {
                scrapeService.scrape(ani, force);
            }
            log.info("批量刮削完毕");
        });

        return Result.success("已开始刮削{}个订阅", anis.size());
    }
}
