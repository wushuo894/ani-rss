package ani.rss.comparator;

import ani.rss.entity.Ani;

import java.io.Serializable;
import java.util.Comparator;

public interface AniComparator extends Comparator<Ani>, Serializable {
    @Override
    int compare(Ani o1, Ani o2);
}
