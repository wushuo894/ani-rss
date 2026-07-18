package ani.rss.comparator;

import ani.rss.entity.Ani;
import cn.hutool.core.util.NumberUtil;

public class AniScoreComparator implements AniComparator {
    @Override
    public int compare(Ani o1, Ani o2) {
        return NumberUtil.compare(o2.getScore(), o1.getScore());
    }
}
