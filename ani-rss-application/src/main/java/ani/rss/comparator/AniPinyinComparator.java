package ani.rss.comparator;

import ani.rss.entity.Ani;
import cn.hutool.core.comparator.PinyinComparator;

public class AniPinyinComparator implements AniComparator {

    static final PinyinComparator pinyinComparator = new PinyinComparator();

    @Override
    public int compare(Ani o1, Ani o2) {
        return pinyinComparator.compare(o1.getTitle(), o2.getTitle());
    }
}
