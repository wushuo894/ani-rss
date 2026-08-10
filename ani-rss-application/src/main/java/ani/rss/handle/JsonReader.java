package ani.rss.handle;

import ani.rss.commons.GsonStatic;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class JsonReader {
    private JsonReader(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    private final File jsonFile;

    public static JsonReader getInstance(File jsonFile) {
        return new JsonReader(jsonFile);
    }

    public <T> T toObject(Class<T> tClass) {
        T object = toObject(tClass, null);
        Objects.requireNonNull(object);
        return object;
    }

    public <T> T toObject(Class<T> tClass, T defaultValue) {
        if (jsonFile.exists()) {
            String json = FileUtil.readUtf8String(jsonFile);
            return GsonStatic.fromJson(json, tClass);
        }
        if (Objects.nonNull(defaultValue)) {
            String json = GsonStatic.toJson(defaultValue);
            FileUtil.writeUtf8String(json, jsonFile);
            return defaultValue;
        }
        return null;
    }

    public <T> List<T> toList(Class<T> tClass) {
        List<T> list = toList(tClass, null);
        Objects.requireNonNull(list);
        return list;
    }

    public <T> List<T> toList(Class<T> tClass, List<T> defaultValue) {
        if (jsonFile.exists()) {
            String json = FileUtil.readUtf8String(jsonFile);
            return GsonStatic.fromJsonList(json, tClass);
        }
        String json = GsonStatic.toJson(defaultValue);
        FileUtil.writeUtf8String(json, jsonFile);
        return defaultValue;
    }
}
