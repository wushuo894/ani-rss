package ani.rss;

import ani.rss.other.Cron;
import ani.rss.util.*;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Main {

    public static List<String> ARGS = new ArrayList<>();

    public static void main(String[] args) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Main.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        try {
            ConfigUtil.load();
            ConfigUtil.backup();
            MenuUtil.start();
            ServerUtil.start();

            AniUtil.load();
            TaskUtil.start();
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
