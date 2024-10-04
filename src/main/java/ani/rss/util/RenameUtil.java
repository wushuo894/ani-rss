package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RenameUtil {
    private static final String s = "(.*|\\[.*])( -? \\d+(\\.5)?|\\[\\d+(\\.5)?]|\\[\\d+(\\.5)?.?[vV]\\d]|第\\d+(\\.5)?[话話集]|\\[第?\\d+(\\.5)?[话話集]]|\\[\\d+(\\.5)?.?END]|[Ee][Pp]?\\d+(\\.5)?)(.*)";


    public static Boolean rename(Ani ani, Item item) {
        Config config = ConfigUtil.CONFIG;

        int offset = ani.getOffset();
        int season = ani.getSeason();
        String title = ani.getTitle();
        Boolean customEpisode = ani.getCustomEpisode();
        String customEpisodeStr = ani.getCustomEpisodeStr();
        Integer customEpisodeGroupIndex = ani.getCustomEpisodeGroupIndex();
        String renameTemplate = config.getRenameTemplate();

        String subgroup = item.getSubgroup();

        String itemTitle = item.getTitle();
        itemTitle = itemTitle.replace("+NCOPED", "");

        String e;
        if (customEpisode) {
            e = ReUtil.get(customEpisodeStr, itemTitle, customEpisodeGroupIndex);
        } else {
            e = ReUtil.get(s, itemTitle, 2);
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

        String reName = renameTemplate;

        item
                .setReName(reName);
        return true;
    }
}
