package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.File;
import java.util.Map;

public class ConfigUtil {
    @Getter
    private final static Config config = new Config();
    private final static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public static File getConfigFile() {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        File configFile = new File("config.json");
        if (StrUtil.isNotBlank(config)) {
            configFile = new File(config + File.separator + "config.json");
        }
        return configFile;
    }

    public static void load() {
        config.setSleep(5)
                .setRename(true)
                .setHost("")
                .setUsername("")
                .setPassword("");
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(gson.toJson(config), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        BeanUtil.copyProperties(gson.fromJson(s, Config.class), config);
    }

    public static void sync() {
        File configFile = getConfigFile();
        String json = gson.toJson(config);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
    }
}
