package ani.rss.util;

import ani.rss.other.TimeZoneSerializer;
import cn.hutool.core.date.DatePattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.TimeZone;

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
        return gson.fromJson(body, tClass);
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

}
