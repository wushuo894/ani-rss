package ani.rss.comparator;

import ani.rss.entity.Ani;

public class AniDownloadTimeComparator implements AniComparator {
    @Override
    public int compare(Ani o1, Ani o2) {
        return Long.compare(o2.getLastDownloadTime(), o1.getLastDownloadTime());
    }
}
