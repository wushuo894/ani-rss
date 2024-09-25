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
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(() -> {
                        ServerUtil.stop();
                        // 直接退出
                        System.exit(3);
                    }));
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
    }

}
