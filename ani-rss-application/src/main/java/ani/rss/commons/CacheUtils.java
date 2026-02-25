package ani.rss.commons;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.FIFOCache;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheUtils {
    static final FIFOCache<Object, Object> CACHE = CacheUtil.newFIFOCache(1024 * 8);

    @Synchronized("CACHE")
    public static <V> V get(Object key) {
        log.debug("get key [{}]", key);
        return (V) CACHE.get(key);
    }

    @Synchronized("CACHE")
    public static void put(Object key, Object object) {
        log.debug("put key [{}]", key);
        CACHE.put(key, object);
    }

    @Synchronized("CACHE")
    public static void put(Object key, Object object, long timeout) {
        log.debug("put key [{}] timeout [{}]", key, timeout);
        CACHE.put(key, object, timeout);
    }

    @Synchronized("CACHE")
    public static boolean containsKey(Object key) {
        log.debug("contains key [{}]", key);
        return CACHE.containsKey(key);
    }

    @Synchronized("CACHE")
    public static void remove(Object key) {
        log.debug("remove key [{}]", key);
        CACHE.remove(key);
    }
}
