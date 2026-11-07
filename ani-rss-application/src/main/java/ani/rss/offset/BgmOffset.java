package ani.rss.offset;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmEpisodes;
import ani.rss.util.other.BgmUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跟随 Bangumi 正篇编号的偏移策略，使用对应条目最小正篇集数作为目标首集。
 * <p>
 * 例如 Bangumi 正篇从第 13 集开始时，RSS 从第 1 集开始则偏移 +12，
 * RSS 从第 13 集开始则保持编号。
 */
public class BgmOffset implements BaseOffset {
    /**
     * 当前策略实例内的目标首集缓存，以订阅对象为键保存查询结果。
     */
    private final Map<Ani, Integer> map = new HashMap<>();

    /**
     * 根据订阅的 Bangumi 链接获取最小正篇集数，优先读取当前实例的缓存。
     * <p>
     * 正篇列表为空时以第 1 集为起点；获取作品或剧集信息时的异常由调用方处理。
     *
     * @param ani 包含 Bangumi 作品链接的订阅
     * @return Bangumi 正篇最小集数，正篇列表为空时返回 1
     */
    @Override
    public Integer getMinEpisode(Ani ani) {
        if (map.containsKey(ani)) {
            return map.get(ani);
        }

        String subjectId = BgmUtil.getSubjectId(ani);
        // Bangumi 的剧集类型 0 表示正篇，避免将特别篇等其他类型作为起始集。
        List<BgmEpisodes.BgmEpisode> episodes = BgmUtil.getEpisodes(subjectId, 0);
        int minEpisode = episodes
                .stream()
                .mapToInt(BgmEpisodes.BgmEpisode::getSort)
                .min()
                .orElse(1);
        map.put(ani, minEpisode);
        return minEpisode;
    }
}
