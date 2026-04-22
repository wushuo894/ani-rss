package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.AniBT;
import ani.rss.entity.web.Result;
import ani.rss.util.other.AniBTUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class AniBTController {

    @Resource
    private MikanController mikanController;

    @Auth
    @Operation(summary = "AniBT 番剧列表")
    @PostMapping("/aniBT")
    public Result<AniBT> aniBT(HttpServletRequest request) {
        String season = request.getParameter("season");
        String bgmUrl = request.getParameter("bgmUrl");
        return Result.success(AniBTUtil.list(season, bgmUrl));
    }

    @Auth
    @PostMapping("/aniBTGroup")
    public Result<List<AniBT.Group>> aniBTGroup(@RequestParam("bgmId") String bgmId) {
        List<AniBT.Group> groups = AniBTUtil.getGroups(bgmId);

        List<String> regexItemList = List.of(
                "1920[Xx]1080", "3840[Xx]2160", "1080[Pp]", "4[Kk]", "720[Pp]",
                "繁", "简", "日",
                "cht|Cht|CHT", "chs|Chs|CHS", "hevc|Hevc|HEVC",
                "10bit|10Bit|10BIT", "h265|H265", "h264|H264",
                "内嵌", "内封", "外挂",
                "mp4|MP4", "mkv|MKV"
        );

        for (AniBT.Group group : groups) {
            Set<String> tags = new HashSet<>();
            List<List<AniBT.RegexItem>> regexList = new ArrayList<>();
            List<AniBT.Item> items = group.getItems();
            for (AniBT.Item item : items) {
                String title = item.getTitle();
                List<AniBT.RegexItem> regexItems = new ArrayList<>();
                for (String regex : regexItemList) {
                    if (!ReUtil.contains(regex, title)) {
                        continue;
                    }
                    String label = ReUtil.get(regex, title, 0);
                    label = label.toUpperCase();
                    AniBT.RegexItem regexItem = new AniBT.RegexItem(label, regex);
                    regexItems.add(regexItem);
                    tags.add(label);
                }
                regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
                regexList.add(regexItems);
            }

            regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);
            group
                    .setBgmId(bgmId)
                    .setRegexList(regexList)
                    .setTags(tags);
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
