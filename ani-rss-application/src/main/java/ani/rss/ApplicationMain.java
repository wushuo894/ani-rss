package ani.rss;

import ani.rss.commons.ExceptionUtil;
import ani.rss.commons.MavenUtil;
import ani.rss.entity.Global;
import ani.rss.other.Cron;
import ani.rss.service.TaskService;
import ani.rss.util.ServerUtil;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.MenuUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ApplicationMain {

    public static void main(String[] args) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Global.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        try {
            ConfigUtil.load();
            ConfigUtil.backup();
            MenuUtil.start();
            ServerUtil.start();

            AniUtil.load();
            TaskService.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            Cron.start();
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
    }

}
