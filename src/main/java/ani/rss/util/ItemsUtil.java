package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.enums.MessageEnum;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ItemsUtil {

    static Cache<String, String> messageCache = CacheUtil.newFIFOCache(40960);

    /**
     * 检测是否缺集
     *
     * @param ani
     * @param items
     */
    public static synchronized void omit(Ani ani, List<Item> items) {
        Config config = ConfigUtil.CONFIG;
        Boolean omit = config.getOmit();
        if (!omit) {
            return;
        }
        if (items.isEmpty()) {
            return;
        }

        if (!ani.getOmit()) {
            return;
        }

        Boolean ova = ani.getOva();
        if (ova) {
            return;
        }

        int[] array = items.stream().mapToInt(o -> o.getEpisode().intValue()).distinct().toArray();
        int max = ArrayUtil.max(array);
        int min = ArrayUtil.min(array);
        if (max == min) {
            return;
        }
        Integer season = ani.getSeason();
        String title = ani.getTitle();

        for (int i = min; i <= max; i++) {
            if (ArrayUtil.contains(array, i)) {
                continue;
            }
            String s = StrFormatter.format("缺少集数 {} S{}E{}", title, String.format("%02d", season), String.format("%02d", i));
            if (messageCache.containsKey(s)) {
                continue;
            }
            log.info(s);
            // 缓存一天 不重复发送
            messageCache.put(s, "1", TimeUnit.DAYS.toMillis(1));
            MessageUtil.send(config, ani, s, MessageEnum.OMIT);
        }
    }

    public static int currentEpisodeNumber(Ani ani, List<Item> items) {
        if (items.isEmpty()) {
            return 0;
        }

        int currentEpisodeNumber;
        Boolean downloadNew = ani.getDownloadNew();
        if (downloadNew) {
            currentEpisodeNumber = items
                    .stream()
                    .filter(it -> it.getEpisode() == it.getEpisode().intValue())
                    .mapToInt(item -> item.getEpisode().intValue())
                    .max().orElse(0);
        } else {
            currentEpisodeNumber = (int) items
                    .stream()
                    .filter(it -> it.getEpisode() == it.getEpisode().intValue()).count();
        }
        return currentEpisodeNumber;
    }

}
