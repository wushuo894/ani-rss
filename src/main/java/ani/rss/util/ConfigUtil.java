package ani.rss.util;

import ani.rss.entity.Config;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class ConfigUtil {

    @Getter
    private static final Config CONFIG = new Config();

    private static final Gson GSON = new GsonBuilder()
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
                .setDelete(false)
                .setHost("")
                .setUsername("")
                .setPassword("")
                .setDebug(false)
                .setProxy(false)
                .setProxyHost("")
                .setProxyPort(8080);
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(GSON.toJson(CONFIG), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        BeanUtil.copyProperties(GSON.fromJson(s, Config.class), CONFIG, CopyOptions
                .create()
                .setIgnoreNullValue(true));
        loadLogback();
        log.debug("加载配置文件 {}", configFile);
    }

    /**
     * 将设置保存到磁盘
     */
    public static void sync() {
        File configFile = getConfigFile();
        String json = GSON.toJson(CONFIG);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
        loadLogback();
        log.debug("保存配置 {}", configFile);
    }

    public static void loadLogback() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            String s = ResourceUtil.readUtf8Str("logback.xml.template");
            s = s.replace("${config}", ConfigUtil.getConfigDir().toString() + "/");
            Boolean debug = CONFIG.getDebug();
            if (debug) {
                s = s.replace("${level}", "debug");
            } else {
                s = s.replace("${level}", "info");
            }

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();

            @Cleanup
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            configurator.doConfigure(byteArrayInputStream);
        } catch (JoranException e) {
            log.error(e.getMessage());
            log.debug(String.valueOf(e));
        }
    }
}
