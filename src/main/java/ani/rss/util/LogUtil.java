package ani.rss.util;

import ani.rss.entity.Config;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class LogUtil {
    public static void loadLogback() {
        Config config = ConfigUtil.getCONFIG();
        Boolean debug = config.getDebug();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            String s = ResourceUtil.readUtf8Str("logback.xml.template");
            s = s.replace("${config}", ConfigUtil.getConfigDir() + "/");
            if (debug) {
                s = s.replace("${level}", "debug");
            } else {
                s = s.replace("${level}", "info");
            }

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();

            byteArrayInputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
            configurator.doConfigure(byteArrayInputStream);
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug(String.valueOf(e));
        } finally {
            IoUtil.close(byteArrayInputStream);
        }
    }

    public static List<String> getLogs() {
        String filename = ConfigUtil.getConfigDir() + "/logs/ani-rss.log";
        List<String> logs = FileUtil.readUtf8Lines(filename);
        int maxLines = 4096;
        if (logs.size() <= maxLines) {
            return logs;
        }
        return CollUtil.sub(logs, logs.size() - maxLines, logs.size());
    }
}
