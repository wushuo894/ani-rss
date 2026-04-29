package ani.rss.service;

import ani.rss.commons.GsonStatic;
import ani.rss.commons.WeekComparator;
import ani.rss.entity.Ani;
import ani.rss.entity.AniBT;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AniBTService {
    private static final String HOST = "https://site.anibt.net";

    public AniBT list(String season, String bgmUrl) {
        List<String> bgmIdList = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getBgmUrl)
                .filter(StrUtil::isNotBlank)
                .map(BgmUtil::getSubjectId)
                .distinct()
                .toList();

        String bgmId = "";

        if (StrUtil.isNotBlank(bgmUrl)) {
            bgmId = BgmUtil.getSubjectId(bgmUrl);
        }

        AniBT aniBT = HttpReq.get(HOST + "/api/seasons/anime")
                .form("season", season)
                .form("bgmId", bgmId)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonObject data = jsonObject.getAsJsonObject("data");
                    return GsonStatic.fromJson(data, AniBT.class);
                });

        List<AniBT.ByWeekday> byWeekday = aniBT.getByWeekday();

        WeekComparator weekComparator = new WeekComparator();

        byWeekday = byWeekday.stream().sorted((a, b) ->
                weekComparator.compare(a.getWeekdayLabel(), b.getWeekdayLabel())
        ).toList();
        aniBT.setByWeekday(byWeekday);

        for (AniBT.ByWeekday weekday : byWeekday) {
            List<AniBT.Anime> animes = weekday.getAnimes();
            for (AniBT.Anime anime : animes) {
                boolean exists = bgmIdList.contains(anime.getBgmId());
                anime.setExists(exists);
            }
        }

        return aniBT;
    }

    public List<AniBT.Group> getGroups(String bgmId) {
        return HttpReq.get(HOST + "/api/anime/groups")
                .form("bgmId", bgmId)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray groups = jsonObject.getAsJsonObject("data")
                            .getAsJsonArray("groups");
                    List<AniBT.Group> groupList = GsonStatic.fromJsonList(groups, AniBT.Group.class);
                    for (AniBT.Group group : groupList) {
                        String slug = group.getSlug();
                        String rss = "https://anibt.net/rss/anime.xml?bgmId={}&groupSlug={}";
                        rss = StrUtil.format(rss, bgmId, slug);
                        group.setRss(rss);
                    }
                    return groupList;
                });
    }
}
