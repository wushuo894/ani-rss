package ani.rss.offset;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.util.other.ItemsUtil;

import java.util.List;

public interface BaseOffset {
    /**
     * 根据目标正篇起始集数计算偏移，小数集保留在首集之前。
     */
    default Integer getOffset(Ani ani, List<Item> items) {
        if (items.isEmpty()) {
            return 0;
        }

        Double itemMinEpisode = items.stream()
                .mapToDouble(Item::getEpisode)
                .min()
                .orElse(0.0);

        int minEpisode = getMinEpisode(ani);

        if (minEpisode > 1) {
            ani.setSeason(1);
        }

        if (ItemsUtil.is5(itemMinEpisode)) {
            // 例如目标首集为 13 时，将 0.5 或 12.5 对齐到 12.5。
            return minEpisode - 1 - itemMinEpisode.intValue();
        }

        return -(itemMinEpisode.intValue() - minEpisode);
    }

    Integer getMinEpisode(Ani ani);
}
