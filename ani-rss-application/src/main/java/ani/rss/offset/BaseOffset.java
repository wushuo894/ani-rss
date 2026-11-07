package ani.rss.offset;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.other.ItemsUtil;

import java.util.List;

/**
 * 自动剧集偏移策略，按目标正篇起始集数对齐 RSS 中的最小集数。
 * <p>
 * 实现类负责提供目标起始集数，共用默认方法计算整数偏移量并处理小数集。
 */
public interface BaseOffset {
    /**
     * 根据 RSS 最小集数与策略提供的目标起始集数计算偏移。
     * <p>
     * 整数集对齐到目标首集，x.5 集对齐到目标首集的前半集。
     * 条目为空时直接返回 0；条目非空且目标首集大于 1 时，将订阅的季设为 1。
     *
     * @param ani   待计算偏移的订阅，计算过程中可能调整其季编号
     * @param items RSS 解析得到的可用条目，使用其中最小集数进行计算
     * @return 应加到条目集数上的整数偏移量，条目为空时返回 0
     */
    default Integer getOffset(Ani ani, List<Item> items) {
        if (items.isEmpty()) {
            return 0;
        }

        // 以最小可用集数为基准，结果取决于 RSS 当前保留的条目。
        Double itemMinEpisode = items.stream()
                .mapToDouble(Item::getEpisode)
                .min()
                .orElse(0.0);

        int minEpisode = getMinEpisode(ani);

        if (minEpisode > 1) {
            // 目标采用跨季连续编号时，统一使用第 1 季承载连续集数。
            ani.setSeason(1);
        }

        if (ItemsUtil.is5(itemMinEpisode)) {
            // 例如目标首集为 13 时，将 0.5 或 12.5 对齐到 12.5。
            return minEpisode - 1 - itemMinEpisode.intValue();
        }

        // 整数集直接平移到目标首集，例如 RSS 从 25 开始、目标为 13 时偏移 -12。
        return -(itemMinEpisode.intValue() - minEpisode);
    }

    /**
     * 获取当前策略要求的目标正篇起始集数。
     *
     * @param ani 用于确定目标集数的订阅，可由实现类读取其作品信息
     * @return 偏移后正篇应采用的起始集数
     */
    Integer getMinEpisode(Ani ani);
}
