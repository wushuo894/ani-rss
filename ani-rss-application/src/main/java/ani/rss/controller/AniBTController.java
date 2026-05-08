package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.GroupRegexUtils;
import ani.rss.entity.AniBT;
import ani.rss.entity.GroupRegex;
import ani.rss.entity.web.Result;
import ani.rss.service.AniBTService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AniBTController {

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
            GroupRegex groupRegx = GroupRegexUtils.toGroupRegx(items, AniBT.Item::getTitle);

            group.setBgmId(bgmId)
                    .setGroupRegex(groupRegx);
        }
        return Result.success(groups);
    }
}
