package ani.rss.list;

import java.util.LinkedList;

public class FixedSizeLinkedList  <T> extends LinkedList<T> {
    private final int maxSize;

    public FixedSizeLinkedList(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T t) {
        boolean r = super.add(t);
        if (size() > maxSize) {
            removeRange(0, size() - maxSize);
        }
        return r;
    }
}
