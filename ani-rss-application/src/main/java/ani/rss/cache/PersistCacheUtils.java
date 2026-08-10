package ani.rss.cache;

import ani.rss.handle.JsonFile;
import lombok.Synchronized;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class PersistCacheUtils {

    private static final Map<File, PersistCacheUtils> MAP = new ConcurrentHashMap<>();

    private final Map<String, CacheObject> cacheObjectMap = new ConcurrentHashMap<>();

    private PersistCacheUtils(File jsonFile) {
        this.jsonFile = JsonFile.getInstance(jsonFile);
        init();
    }

    private final JsonFile jsonFile;

    private void init() {
        List<CacheObject> list = jsonFile.toList(CacheObject.class, new ArrayList<>());
        for (CacheObject cacheObject : list) {
            String key = cacheObject.getKey();
            cacheObjectMap.put(key, cacheObject);
        }
    }

    public static PersistCacheUtils getInstance(File jsonFile) {
        return MAP.computeIfAbsent(jsonFile, PersistCacheUtils::new);
    }

    public void put(String key, Object value) {
        put(key, value, 0L);
    }

    @Synchronized("jsonFile")
    public void put(String key, Object value, Long time) {
        CacheObject cacheObject = new CacheObject(time, key, value);
        cacheObjectMap.put(key, cacheObject);
        sync();
    }

    @Synchronized("jsonFile")
    public void remove(String key) {
        cacheObjectMap.remove(key);
        sync();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheObject cacheObject = cacheObjectMap.get(key);
        if (Objects.isNull(cacheObject) || cacheObject.expires()) {
            return null;
        }
        return (T) cacheObject.getValue();
    }

    @Synchronized("jsonFile")
    public void sync() {
        cacheObjectMap.entrySet().removeIf(entry ->
                entry.getValue().expires()
        );
        jsonFile.writer(new ArrayList<>(cacheObjectMap.values()));
    }

}
