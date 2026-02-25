package ani.rss.commons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.TimeZone;

public class TimeZoneSerializer implements JsonSerializer<TimeZone> {
    @Override
    public JsonElement serialize(TimeZone src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", src.getID());
        return jsonObject;
    }
}
