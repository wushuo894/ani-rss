package ani.rss.offset;

import ani.rss.entity.Ani;

/**
 * 从第 1 集开始编号的自动偏移策略，沿用原有的自动偏移规则。
 * <p>
 * 最小整数集数对齐到第 1 集；最小集数为 x.5 时对齐到 0.5。
 */
public class AutoOffset implements BaseOffset {
    /**
     * 获取固定的目标正篇起始集数。
     *
     * @param ani 待计算偏移的订阅，此策略不依赖其作品信息
     * @return 固定返回 1
     */
    @Override
    public Integer getMinEpisode(Ani ani) {
        return 1;
    }
}
