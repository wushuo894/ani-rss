package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Mikan;
import ani.rss.entity.web.Result;
import ani.rss.service.MikanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MikanController extends BaseController {

    @Resource
    private MikanService mikanService;

    @Auth
    @Operation(summary = "获取Mikan番剧列表")
    @PostMapping("/mikan")
    public Result<Mikan> mikan(@RequestParam("text") String text, @RequestBody Mikan.Season season) {
        Mikan list = mikanService.list(text, season);
        return Result.success(list);
    }

    @Auth
    @Operation(summary = "获取Mikan番剧的字幕组列表")
    @PostMapping("/mikanGroup")
    public Result<List<Mikan.Group>> mikanGroup(@RequestParam("url") String url) {
        List<Mikan.Group> groups = mikanService.getGroups(url);
        return Result.success(groups);
    }
}
