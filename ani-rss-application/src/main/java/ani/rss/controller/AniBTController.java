package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.AniBT;
import ani.rss.entity.web.Result;
import ani.rss.service.AniBTService;
import ani.rss.util.other.SourceSelectorUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AniBTController {

    @Resource
    private MikanController mikanController;

    @Resource
    private AniBTService aniBTService;

    @Auth
    @Operation(summary = "AniBT 番剧列表")
    @PostMapping("/aniBT")
    public Result<AniBT> aniBT(HttpServletRequest request) {
        String season = request.getParameter("season");
        String bgmUrl = request.getParameter("bgmUrl");
        return Result.success(aniBTService.list(season, bgmUrl));
    }

    @Auth
    @PostMapping("/aniBTGroup")
    public Result<List<AniBT.Group>> aniBTGroup(@RequestParam("bgmId") String bgmId) {
        List<AniBT.Group> groups = aniBTService.getGroups(bgmId);

        for (AniBT.Group group : groups) {
            List<AniBT.Item> items = group.getItems();
            SourceSelectorUtil.SelectorData<AniBT.RegexItem> selectorData = SourceSelectorUtil.build(
                    items.stream().map(AniBT.Item::getTitle).toList(),
                    AniBT.RegexItem::new
            );
            group
                    .setBgmId(bgmId)
                    .setRegexList(selectorData.regexList())
                    .setTags(selectorData.tags());
        }
        return Result.success(groups);
    }


    @Auth
    @Operation(summary = "获取AniBT封面")
    @GetMapping("/aniBTCover")
    public void aniBTCover(@RequestParam("img") String img) {
        mikanController.mikanCover(img);
    }
}
