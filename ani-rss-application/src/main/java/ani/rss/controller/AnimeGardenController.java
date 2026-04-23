package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.AnimeGarden;
import ani.rss.entity.web.Result;
import ani.rss.util.other.AnimeGardenUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class AnimeGardenController {

    @Auth
    @Operation(summary = "AnimeGarden 番剧列表")
    @PostMapping("/animeGardenList")
    public Result<List<AnimeGarden.Week>> animeGardenList(HttpServletRequest request) {
        String bgmUrl = request.getParameter("bgmUrl");
        return Result.success(AnimeGardenUtil.list(bgmUrl));
    }

    @Auth
    @Operation(summary = "AnimeGarden 番剧字幕组列表")
    @PostMapping("/animeGardenGroup")
    public Result<List<AnimeGarden.Group>> animeGardenGroup(@RequestParam("bgmId") String bgmId) {
        return Result.success(AnimeGardenUtil.group(bgmId));
    }
}
