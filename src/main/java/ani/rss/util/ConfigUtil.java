package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.File;
import java.util.Map;

public class ConfigUtil {
    @Getter
    private final static Config CONFIG = new Config();
    private final static Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    /**
     * 获取设置文件夹
     *
     * @return
     */
    public static File getConfigDir() {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        return new File(config).getAbsoluteFile();
    }

    /**
     * 获取设置文件
     *
     * @return
     */
    public static File getConfigFile() {
        File configDir = getConfigDir();
        return new File(configDir + File.separator + "config.json");
    }

    /**
     * 加载设置
     */
    public static void load() {
        CONFIG.setSleep(5)
                .setRename(true)
                .setFileExist(false)
                .setHost("")
                .setUsername("")
                .setPassword("");
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(GSON.toJson(CONFIG), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        BeanUtil.copyProperties(GSON.fromJson(s, Config.class), CONFIG);
    }

    /**
     * 将设置保存到磁盘
     */
    public static void sync() {
        File configFile = getConfigFile();
        String json = GSON.toJson(CONFIG);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
    }
}
