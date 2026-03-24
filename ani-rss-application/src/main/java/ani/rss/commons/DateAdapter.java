package ani.rss.commons;

import cn.hutool.core.date.DatePattern;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {
    private final DateFormat dateFormat = new SimpleDateFormat(DatePattern.NORM_DATE_PATTERN);

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(dateFormat.format(src));
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return dateFormat.parse(json.getAsString());
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }
}