package ani.rss.handle;

import java.io.File;
import java.util.List;

public class JsonFile {
    private final JsonReader jsonReader;
    private final JsonWriter jsonWriter;

    private JsonFile(File jsonFile) {
        this.jsonReader = JsonReader.getInstance(jsonFile);
        this.jsonWriter = JsonWriter.getInstance(jsonFile);
    }

    public static JsonFile getInstance(File jsonFile) {
        return new JsonFile(jsonFile);
    }

    public <T> T toObject(Class<T> tClass) {
        return jsonReader.toObject(tClass);
    }

    public <T> T toObject(Class<T> tClass, T defaultValue) {
        return jsonReader.toObject(tClass, defaultValue);
    }

    public <T> List<T> toList(Class<T> tClass) {
        return jsonReader.toList(tClass);
    }

    public <T> List<T> toList(Class<T> tClass, List<T> defaultValue) {
        return jsonReader.toList(tClass, defaultValue);
    }

    public void writer(Object object) {
        jsonWriter.writer(object);
    }

}
