package ani.rss;

import ani.rss.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        try {
            UpdateUtil.manageWindows();
            ConfigUtil.load();
            AniUtil.load();
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            MenuUtil.start(args);
            ServerUtil.create(args).start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

}
