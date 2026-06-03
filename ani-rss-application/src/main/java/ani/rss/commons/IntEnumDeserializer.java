package ani.rss.commons;

import ani.rss.entity.IntEnum;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class IntEnumDeserializer implements JsonDeserializer<IntEnum> {

    private static final Map<Class<?>, Map<Integer, IntEnum>> CACHE = new HashMap<>();

    @Override
    public IntEnum deserialize(
            JsonElement json,
            Type type,
            JsonDeserializationContext context) {

        int code = json.getAsInt();
        Class<?> clazz = (Class<?>) type;

        Map<Integer, IntEnum> map = CACHE.computeIfAbsent(clazz, k -> {
            Map<Integer, IntEnum> m = new HashMap<>();
            for (Object e : clazz.getEnumConstants()) {
                IntEnum intEnum = (IntEnum) e;
                m.put(intEnum.getCode(), intEnum);
            }
            return m;
        });

        IntEnum result = map.get(code);
        if (result == null) {
            throw new JsonParseException(
                    "Unknown code " + code + " for enum " + clazz.getSimpleName());
        }
        return result;
    }
}
