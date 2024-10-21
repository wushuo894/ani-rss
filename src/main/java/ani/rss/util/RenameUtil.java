package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.enums.StringEnum;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class RenameUtil {
    public static final String REG_STR;

    static {
        String s = ResourceUtil.readUtf8Str("reg.json");
        JsonArray jsonElements = new Gson().fromJson(s, JsonArray.class);
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

        String episode = ReUtil.get("\\d+(\\.5)?", e, 0);
        if (StrUtil.isBlank(episode)) {
            return false;
        }

        Boolean skip5 = config.getSkip5();
        if (skip5) {
            if (episode.endsWith(".5")) {
                log.debug("{} 疑似 {} 剧集, 自动跳过", itemTitle, episode + ".5");
                return false;
            }
        }

        boolean is5 = Double.parseDouble(episode) - 0.5 == Double.valueOf(episode).intValue();

        item.setEpisode(Double.parseDouble(episode) + offset);

        String seasonFormat = String.format("%02d", season);
        String episodeFormat = String.format("%02d", item.getEpisode().intValue());

        if (is5) {
            episodeFormat = episodeFormat + ".5";
        }

        renameTemplate = renameTemplate.replace("${title}", title);
        renameTemplate = renameTemplate.replace("${seasonFormat}", seasonFormat);
        renameTemplate = renameTemplate.replace("${episodeFormat}", episodeFormat);
        renameTemplate = renameTemplate.replace("${season}", String.valueOf(season));
        renameTemplate = renameTemplate.replace("${episode}", episode);
        renameTemplate = renameTemplate.replace("${subgroup}", subgroup);
        renameTemplate = renameTemplate.replace("${itemTitle}", itemTitle);

        String reName = renameTemplate.trim();

        item
                .setReName(reName);
        return true;
    }

}
