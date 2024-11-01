package ani.rss.util;


import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;

/**
 * 使用ehcache持久化存储
 */
@Slf4j
public class EhCacheUtil {

    public static final Cache<String, String> MY_CACHE;

    static {
        CacheConfigurationBuilder<String, String> builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(50, MemoryUnit.MB)
                        .disk(100, MemoryUnit.MB, true));
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File(ConfigUtil.getConfigDir() + "/cache")))
                .withCache("myCache", builder)
                .build();
        cacheManager.init();

        MY_CACHE = cacheManager.getCache("myCache", String.class, String.class);
        RuntimeUtil.addShutdownHook(() -> {
            try {
                cacheManager.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    public static void put(String key, String object) {
        MY_CACHE.putIfAbsent(key, object);
    }

    public static String get(String key) {
        return MY_CACHE.get(key);
    }

    public static void remove(String key) {
        MY_CACHE.remove(key);
    }

}
