package ani.rss.commons;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class WeekComparator implements Comparator<String>, Serializable {
    private static final List<String> WEEK_ORDER = List.of(
            "(星期|周)日", "(星期|周)一", "(星期|周)二", "(星期|周)三", "(星期|周)四", "(星期|周)五", "(星期|周)六"
    );

    private static List<String> WEEK_SORT = new ArrayList<>();

    public WeekComparator() {
        WEEK_SORT = getIndexList();
    }

    private List<String> getIndexList() {
        List<String> weekList = new ArrayList<>();
        int dayOfWeek = DateUtil.dayOfWeek(new Date()) - 1;

        for (int i = dayOfWeek; i >= 0; i--) {
            weekList.add(WEEK_ORDER.get(i));
        }

        for (int i = 6; i > 0; i--) {
            String s = WEEK_ORDER.get(i);
            if (weekList.contains(s)) {
                continue;
            }
            weekList.add(s);
        }
        return weekList;
    }

    private int getIndexOf(String s) {
        for (int i = 0; i < WEEK_SORT.size(); i++) {
            if (ReUtil.contains(WEEK_SORT.get(i), s)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int compare(String str1, String str2) {
        int index1 = getIndexOf(str1);
        int index2 = getIndexOf(str2);
        return Integer.compare(index1, index2);
    }
}
