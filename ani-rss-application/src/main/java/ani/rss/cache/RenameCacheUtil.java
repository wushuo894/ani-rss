package ani.rss.cache;


import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;
import java.util.Objects;

/**
 * 重命名缓存
 */
@Slf4j
public class RenameCacheUtil {
    private volatile static PersistCacheUtils persistCacheUtils;

    private synchronized static void init() {
        if (Objects.nonNull(persistCacheUtils)) {
            return;
        }
        File configDir = ConfigUtil.getConfigDir();
        File renameCacheFile = new File(configDir, "cache/rename-cache.json");
        persistCacheUtils = PersistCacheUtils.getInstance(renameCacheFile);
    }

    public static void put(String key, String object) {
        init();
        long time = DateUtil.offsetHour(new Date(), 1).getTime();
        persistCacheUtils.put(key, object, time);
    }

    public static String get(String key) {
        init();
        return persistCacheUtils.get(key);
    }

    public static void remove(String key) {
        init();
        persistCacheUtils.remove(key);
    }

}
