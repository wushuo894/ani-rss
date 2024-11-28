package ani.rss.util;

import ani.rss.other.TimeZoneSerializer;
import cn.hutool.core.date.DatePattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;

@Slf4j
public class GsonStatic {
    public static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setDateFormat(DatePattern.NORM_DATETIME_PATTERN)
            .registerTypeAdapter(TimeZone.class, new TimeZoneSerializer())
            .create();

    public static <T> T fromJson(JsonElement jsonElement, Class<T> clazz) {
        return gson.fromJson(jsonElement, clazz);
    }

    public static <T> T fromJson(String body, Class<T> tClass) {
        try {
            return gson.fromJson(body, tClass);
        } catch (Exception e) {
            log.error("JSON 错误: {}", body);
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

}
