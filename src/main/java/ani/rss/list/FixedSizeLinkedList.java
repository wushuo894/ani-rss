package ani.rss.list;

import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;

import java.util.LinkedList;

/**
 * 用于存放日志
 *
 * @param <T>
 */
public class FixedSizeLinkedList<T> extends LinkedList<T> {

    public FixedSizeLinkedList() {
        super();
    }

    @Override
    public boolean add(T t) {
        boolean r = super.add(t);
        Config config = ConfigUtil.CONFIG;
        int logsMax = config.getLogsMax();

        // 限制最大日志条数为 1024
        if (logsMax > 1024) {
            logsMax = 1024;
            config.setLogsMax(logsMax);
        }

        if (size() > logsMax) {
            removeRange(0, size() - logsMax);
        }
        return r;
    }
}
