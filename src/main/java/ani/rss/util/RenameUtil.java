package ani.rss.util;

import ani.rss.entity.*;
import ani.rss.enums.StringEnum;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

@Slf4j
public class RenameUtil {
    public static final String REG_STR = "(.*|\\[.*])(( - |Vol |[Ee][Pp]?)\\d+(\\.5)?|【\\d+(\\.5)?】|\\[\\d+(\\.5)?( ?[vV]\\d)?( ?END)?( ?完)?]|第\\d+(\\.5)?[话話集]( - END)?|^\\[TOC].* \\d+)";

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
        String renameTemplate = getRenameTemplate(ani);

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

        itemTitle = getName(itemTitle);

        String resolution = getResolution(itemTitle);
        String tmdbId = Optional.ofNullable(ani.getTmdb())
                .map(Tmdb::getId)
                .orElse("");

        renameTemplate = renameTemplate.replace("${seasonFormat}", seasonFormat);
        renameTemplate = renameTemplate.replace("${episodeFormat}", episodeFormat);
        renameTemplate = renameTemplate.replace("${season}", String.valueOf(season));
        renameTemplate = renameTemplate.replace("${episode}", episodeStr);
        renameTemplate = renameTemplate.replace("${subgroup}", subgroup);
        renameTemplate = renameTemplate.replace("${itemTitle}", itemTitle);
        renameTemplate = renameTemplate.replace("${resolution}", resolution);
        renameTemplate = renameTemplate.replace("${tmdbid}", tmdbId);
        renameTemplate = renameTemplate.replace("${title}", title);

        renameTemplate = replaceEpisodeTitle(renameTemplate, episode, ani);

        if (renameTemplate.contains("${jpTitle}")) {
            String jpTitle = getJpTitle(ani);
            renameTemplate = renameTemplate.replace("${jpTitle}", jpTitle);
        }

        List<Func1<Ani, Object>> list = List.of(
                Ani::getThemoviedbName
        );

        renameTemplate = replaceField(renameTemplate, ani, list);

        String reName = getName(renameTemplate);

        item
                .setReName(reName);
        return true;
    }

    /**
     * 获取重命名模板
     *
     * @param ani
     * @return
     */
    public static String getRenameTemplate(Ani ani) {
        Config config = ConfigUtil.CONFIG;
        String renameTemplate = config.getRenameTemplate();

        Boolean customRenameTemplateEnable = ani.getCustomRenameTemplateEnable();
        String customRenameTemplate = ani.getCustomRenameTemplate();

        if (customRenameTemplateEnable) {
            renameTemplate = customRenameTemplate;
        }

        if (StrUtil.isBlank(renameTemplate)) {
            renameTemplate = "${title} S${seasonFormat}E${episodeFormat}";
        }
        return renameTemplate;
    }

    public static <T> String replaceField(String template, T object, List<Func1<T, Object>> list) {
        if (Objects.isNull(object)) {
            return template;
        }
        for (Func1<T, Object> func1 : list) {
            try {
                String fieldName = LambdaUtil.getFieldName(func1);
                String s = StrFormatter.format("${{}}", fieldName);
                String v = func1.callWithRuntimeException(object).toString();
                template = template.replace(s, v);
            } catch (Exception ignored) {
            }
        }
        return template;
    }

    /**
     * 替换集标题
     *
     * @param template 模板
     * @param episode  集数
     * @param ani      订阅
     * @return 替换结果
     */
    public static String replaceEpisodeTitle(String template, Double episode, Ani ani) {
        boolean is5 = episode != (int) episode.doubleValue();

        Map<Integer, String> episodeTitleMap = new HashMap<>();
        Map<Integer, Function<Boolean, String>> bgmEpisodeTitleMap = new HashMap<>();

        if (template.contains("${episodeTitle}")) {
            episodeTitleMap = TmdbUtil.getEpisodeTitleMap(ani);
        }

        if (template.contains("${bgmEpisodeTitle}") || template.contains("${bgmJpEpisodeTitle}")) {
            bgmEpisodeTitleMap = BgmUtil.getEpisodeTitleMap(ani);
        }

        String defaultEpisodeTitle = "第" + NumberFormatUtil.format(episode, 1, 0) + "集";

        String episodeTitle = is5 ? defaultEpisodeTitle : episodeTitleMap
                .getOrDefault(episode.intValue(), defaultEpisodeTitle);

        String bgmEpisodeTitle = is5 ? defaultEpisodeTitle : bgmEpisodeTitleMap
                .getOrDefault(episode.intValue(), jp -> defaultEpisodeTitle)
                .apply(false);

        String bgmJpEpisodeTitle = is5 ? defaultEpisodeTitle : bgmEpisodeTitleMap
                .getOrDefault(episode.intValue(), jp -> defaultEpisodeTitle)
                .apply(true);

        template = template.replace("${episodeTitle}", episodeTitle);
        template = template.replace("${bgmEpisodeTitle}", bgmEpisodeTitle);
        template = template.replace("${bgmJpEpisodeTitle}", bgmJpEpisodeTitle);

        return template;
    }


    /**
     * 获取bgm日文标题
     *
     * @param ani 订阅
     * @return 日文标题
     */
    public static String getJpTitle(Ani ani) {
        return Opt.ofNullable(ani)
                .map(Ani::getJpTitle)
                .filter(StrUtil::isNotBlank)
                .orElseGet(() -> {
                    BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani, true);
                    String name = bgmInfo.getName();
                    ani.setJpTitle(name);
                    return name;
                });
    }

    /**
     * 获取分辨率
     *
     * @param itemTitle 标题
     * @return 分辨率
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
