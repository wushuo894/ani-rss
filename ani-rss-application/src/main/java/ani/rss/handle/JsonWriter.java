package ani.rss.handle;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import cn.hutool.core.io.FileUtil;

import java.io.File;

public class JsonWriter {

    private JsonWriter(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    private final File jsonFile;

    public static JsonWriter getInstance(File jsonFile) {
        return new JsonWriter(jsonFile);
    }

    public void writer(Object object) {
        File temp = new File(jsonFile + ".temp");
        FileUtil.del(temp);

        String json = GsonStatic.toJson(object);
        FileUtil.writeUtf8String(json, temp);

        FileUtils.move(temp.toPath(), jsonFile.toPath());
    }
}
