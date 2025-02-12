package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.enums.StringEnum;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RenameUtil {
    public static final String REG_STR;

    static {
        String s = ResourceUtil.readUtf8Str("reg.json");
        JsonArray jsonElements = GsonStatic.fromJson(s, JsonArray.class);
        String ss = jsonElements.asList()
                .stream().map(JsonElement::getAsString)
                .collect(Collectors.joining("|"));
        REG_STR = StrFormatter.format("(.*|\\[.*\\])({})(.*)", ss);
    }

    public static Boolean rename(Ani ani, Item item) {
        Config config = ConfigUtil.CONFIG;

        int offset = ani.getOffset();
        int season = ani.getSeason();
        String title = ani.getTitle();
        Boolean ova = ani.getOva();

        if (ova) {
            item.setReName(title);
            return true;
        }

        Boolean renameDelYear = config.getRenameDelYear();
        Boolean renameDelTmdbId = config.getRenameDelTmdbId();

        if (renameDelTmdbId) {
            title = ReUtil.replaceAll(title, StringEnum.TMDB_ID_REG, "")
                    .trim();
        }

        if (renameDelYear) {
            title = ReUtil.replaceAll(title, StringEnum.YEAR_REG, "")
                    .trim();
        }

        Boolean customEpisode = ani.getCustomEpisode();
        String customEpisodeStr = ani.getCustomEpisodeStr();
        Integer customEpisodeGroupIndex = ani.getCustomEpisodeGroupIndex();
        String renameTemplate = config.getRenameTemplate();

        if (StrUtil.isBlank(renameTemplate)) {
            renameTemplate = "${title} S${seasonFormat}E${episodeFormat}";
        }

        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");

        String itemTitle = item.getTitle();
        itemTitle = itemTitle.replace("+NCOPED", "");
        itemTitle = itemTitle.replace("\n", " ");
        itemTitle = itemTitle.replace("\t", " ");

        String e;
        // 是否使用自定义剧规则
        if (customEpisode) {
            e = ReUtil.get(customEpisodeStr, itemTitle, customEpisodeGroupIndex);
        } else {
            e = ReUtil.get(REG_STR, itemTitle, 2);
        }

        if (StrUtil.isBlank(e)) {
            return false;
        }

        String episodeStr = ReUtil.get("\\d+(\\.5)?", e, 0);
        if (StrUtil.isBlank(episodeStr)) {
            return false;
        }

        Boolean skip5 = config.getSkip5();
        if (skip5) {
            if (episodeStr.endsWith(".5")) {
                return false;
            }
        }

        double episode = Double.parseDouble(episodeStr) + offset;
        item.setEpisode(episode);

        String seasonFormat = String.format("%02d", season);
        String episodeFormat = String.format("%02d", (int) episode);

        episodeStr = String.valueOf((int) episode);

        // .5
        boolean is5 = episode != (int) episode;

        if (is5) {
            episodeFormat = episodeFormat + ".5";
            episodeStr = episodeStr + ".5";
        }

        title = getName(title);
        itemTitle = getName(itemTitle);

        String resolution = getResolution(itemTitle);
        TmdbUtil.Tmdb tmdb = ani.getTmdb();

        renameTemplate = renameTemplate.replace("${title}", title);
        renameTemplate = renameTemplate.replace("${seasonFormat}", seasonFormat);
        renameTemplate = renameTemplate.replace("${episodeFormat}", episodeFormat);
        renameTemplate = renameTemplate.replace("${season}", String.valueOf(season));
        renameTemplate = renameTemplate.replace("${episode}", episodeStr);
        renameTemplate = renameTemplate.replace("${subgroup}", subgroup);
        renameTemplate = renameTemplate.replace("${itemTitle}", itemTitle);
        renameTemplate = renameTemplate.replace("${resolution}", resolution);
        renameTemplate = renameTemplate.replace("${tmdbid}", tmdb.getId());

        String reName = renameTemplate.trim();

        item
                .setReName(reName);
        return true;
    }

    /**
     * 获取分辨率
     *
     * @param itemTitle
     * @return
     */
    private static String getResolution(String itemTitle) {
        Map<String, String> stringStringMap = Map.of(
                "1920x1080", "1080p",
                "3840x2160", "2160p",
                "1280x720", "720p"
        );
        for (String s : stringStringMap.keySet()) {
            itemTitle = itemTitle.replace(s, stringStringMap.get(s));
        }

        String resolutionReg = "(720|1080|2160)[Pp]";
        String resolution = "none";
        if (ReUtil.contains(resolutionReg, itemTitle)) {
            resolution = ReUtil.get(resolutionReg, itemTitle, 0).toLowerCase();
        }
        return resolution;
    }

    public static String getName(String s) {
        s = s.replace("1/2", "½");

        Map<String, String> map = Map.of(
                "/", " ",
                "\\", " ",
                ":", "：",
                "?", "？",
                "|", "｜",
                "*", " ",
                "<", " ",
                ">", " ",
                "\"", " "
        );

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            s = s.replace(key, value);
        }
        while (s.contains("  ")) {
            s = s.replace("  ", " ");
        }
        return s.trim();
    }

}
