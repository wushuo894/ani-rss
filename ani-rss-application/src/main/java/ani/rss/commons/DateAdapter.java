package ani.rss.commons;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Objects;

public class DateAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(DateUtil.formatDate(src));
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (Objects.isNull(json) || json.isJsonNull()) {
            return null;
        }

        String s = json.getAsString();

        if (StrUtil.isBlank(s)) {
            return null;
        }

        String regex = "^(19|20)\\d{2}$";
        if (ReUtil.contains(regex, s)) {
            return DateTime.of(s, "yyyy");
        }

        regex = "^(19|20)\\d{2}-(0[1-9]|1[0-2])$";
        if (ReUtil.contains(regex, s)) {
            return DateTime.of(s, "yyyy-MM");
        }

        try {
            return DateUtil.parse(s);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }
}
