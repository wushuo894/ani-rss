package ani.rss.enums;

import ani.rss.comparator.AniComparator;
import ani.rss.comparator.AniDownloadTimeComparator;
import ani.rss.comparator.AniPinyinComparator;
import ani.rss.comparator.AniScoreComparator;
import cn.hutool.core.util.ReflectUtil;

/**
 * 排序方式
 */
public enum AniSortTypeEnum {
    /**
     * 评分
     */
    SCORE(AniScoreComparator.class),
    /**
     * 拼音
     */
    PINYIN(AniPinyinComparator.class),
    /**
     * 最后下载时间
     */
    DOWNLOAD_TIME(AniDownloadTimeComparator.class);

    AniSortTypeEnum(Class<? extends AniComparator> aClass) {
        this.comparator = ReflectUtil.newInstance(aClass);
    }

    public final AniComparator comparator;
}
