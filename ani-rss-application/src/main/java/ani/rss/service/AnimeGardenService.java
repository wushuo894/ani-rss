package ani.rss.service;

import ani.rss.commons.GsonStatic;
import ani.rss.commons.WeekComparator;
import ani.rss.entity.Ani;
import ani.rss.entity.AnimeGarden;
import ani.rss.entity.BgmInfo;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnimeGardenService {
    private static final String HOST = "https://api.animes.garden";

    @Resource
    private CacheService cacheService;

    public List<AnimeGarden.Week> list(String bgmUrl) {
        List<AnimeGarden.Week> weekList = new ArrayList<>();

        if (StrUtil.isNotBlank(bgmUrl)) {
            AnimeGarden.Week week = new AnimeGarden.Week();
            weekList.add(week);

            String bgmId = BgmUtil.getSubjectId(bgmUrl);
            BgmInfo bgmInfo = BgmUtil.getBgmInfo(bgmId);
            String name = BgmUtil.getFinalName(bgmInfo);

            AnimeGarden.Subject subject = new AnimeGarden.Subject();
            subject.setName(name)
                    .setId(bgmId)
                    .setExists(true);

            week.setWeekLabel("搜索")
                    .setSubjects(List.of(subject));
            return weekList;
        }

        JsonObject bgmScore = cacheService.getBgmScore();
        JsonObject bgmCover = cacheService.getBgmCover();

        List<AnimeGarden.Subject> subjectList = HttpReq.get(HOST + "/subjects")
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray subjects = jsonObject.getAsJsonArray("subjects");
                    return GsonStatic.fromJsonList(subjects, AnimeGarden.Subject.class);
                });

        List<String> bgmIdList = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getBgmUrl)
                .filter(StrUtil::isNotBlank)
                .map(BgmUtil::getSubjectId)
                .distinct()
                .toList();

        for (AnimeGarden.Subject subject : subjectList) {
            String id = subject.getId();

            Double score = Optional.ofNullable(bgmScore.get(id))
                    .map(JsonElement::getAsDouble)
                    .orElse(0.0);

            String cover = Optional.ofNullable(bgmCover.get(id))
                    .map(it -> GsonStatic.fromJson(it, BgmInfo.Images.class))
                    .map(BgmInfo.Images::getSmall)
                    .orElse("");

            boolean exists = bgmIdList.contains(subject.getId());

            subject
                    .setScore(score)
                    .setCover(cover)
                    .setExists(exists);
        }

        List<String> weeks = List.of("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六");

        Map<String, List<AnimeGarden.Subject>> map = subjectList.stream()
                .peek(subject -> {
                    Date activedAt = subject.getActivedAt();
                    int i = DateUtil.dayOfWeek(activedAt) - 1;
                    String weekLabel = weeks.get(i);
                    subject.setWeekLabel(weekLabel);
                })
                .collect(Collectors.groupingBy(AnimeGarden.Subject::getWeekLabel));

        for (String weekLabel : weeks) {
            if (!map.containsKey(weekLabel)) {
                continue;
            }

            AnimeGarden.Week week = new AnimeGarden.Week();
            week.setWeekLabel(weekLabel)
                    .setSubjects(map.get(weekLabel));
            weekList.add(week);
        }

        WeekComparator weekComparator = new WeekComparator();
        weekList = weekList.stream()
                .sorted((a, b) ->
                        weekComparator.compare(a.getWeekLabel(), b.getWeekLabel())
                ).toList();

        return weekList;
    }

    public List<AnimeGarden.Group> group(String bgmId) {
        List<AnimeGarden.Item> items = HttpReq.get(HOST + "/resources")
                .form("subject", bgmId)
                .form("pageSize", 200)
                .form("duplicate", false)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray resources = jsonObject.getAsJsonArray("resources");
                    return GsonStatic.fromJsonList(resources, AnimeGarden.Item.class);
                });

        items = items
                .stream()
                .filter(it -> {
                    AnimeGarden.Fansub fansub = it.getFansub();
                    return Objects.nonNull(fansub);
                })
                .toList();


        Map<String, List<AnimeGarden.Item>> groupIdMap = items.stream()
                .collect(Collectors.groupingBy(it -> it.getFansub().getId()));

        List<AnimeGarden.Group> list = items
                .stream()
                .map(it -> {
                    AnimeGarden.Fansub fansub = it.getFansub();
                    String id = fansub.getId();
                    String name = fansub.getName();
                    Date createdAt = it.getCreatedAt();

                    String rss = StrUtil.format(
                            "{}/feed.xml?subject={}&fansub={}",
                            HOST,
                            bgmId,
                            name.replace("&", "%26")
                    );

                    return new AnimeGarden.Group()
                            .setId(id)
                            .setName(name)
                            .setLastUpdatedAt(createdAt)
                            .setRss(rss)
                            .setBgmId(bgmId);
                })
                .sorted(Comparator.comparing(AnimeGarden.Group::getLastUpdatedAt).reversed())
                .toList();

        list = CollUtil.distinct(list, AnimeGarden.Group::getId, false);


        List<String> regexItemList = List.of(
                "1920[Xx]1080", "3840[Xx]2160", "1080[Pp]", "4[Kk]", "720[Pp]",
                "繁", "简", "日",
                "cht|Cht|CHT", "chs|Chs|CHS", "hevc|Hevc|HEVC",
                "10bit|10Bit|10BIT", "h265|H265", "h264|H264",
                "内嵌", "内封", "外挂",
                "mp4|MP4", "mkv|MKV"
        );

        for (AnimeGarden.Group group : list) {
            String id = group.getId();
            List<AnimeGarden.Item> itemList = groupIdMap.get(id);
            group.setItems(itemList);


            Set<String> tags = new HashSet<>();
            List<List<AnimeGarden.RegexItem>> regexList = new ArrayList<>();
            for (AnimeGarden.Item item : itemList) {
                String title = item.getTitle();
                List<AnimeGarden.RegexItem> regexItems = new ArrayList<>();
                for (String regex : regexItemList) {
                    if (!ReUtil.contains(regex, title)) {
                        continue;
                    }
                    String label = ReUtil.get(regex, title, 0);
                    label = label.toUpperCase();
                    AnimeGarden.RegexItem regexItem = new AnimeGarden.RegexItem(label, regex);
                    regexItems.add(regexItem);
                    tags.add(label);
                }
                regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
                regexList.add(regexItems);
            }

            regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);
            group.setRegexList(regexList)
                    .setTags(tags);

        }

        return list;
    }
}
