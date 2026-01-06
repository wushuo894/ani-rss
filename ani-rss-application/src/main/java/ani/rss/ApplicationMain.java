package ani.rss;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.entity.Global;
import ani.rss.other.Cron;
import ani.rss.service.TaskService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.MenuUtil;
import ani.rss.web.util.ServerUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.Security;
import java.util.List;

@Slf4j
public class ApplicationMain {

    public static void main(String[] args) {
        Global.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        loadProperty();
        try {
            ConfigUtil.load();
            ConfigUtil.backup();
            MenuUtil.start();
            ServerUtil.start();

            AniUtil.load();
            TaskService.start();
            String version = MavenUtils.getVersion();
            log.info("version {}", version);

            Cron.start();
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
        RuntimeUtil.addShutdownHook(() -> log.info("程序退出..."));
    }

    public static void loadProperty() {
        // 启用Basic认证
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // DNS解析成功过期时间
        Security.setProperty("networkaddress.cache.ttl", "30");
        // DNS解析失败过期时间
        Security.setProperty("networkaddress.cache.negative.ttl", "5");
    }

}
