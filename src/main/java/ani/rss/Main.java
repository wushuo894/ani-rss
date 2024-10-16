package ani.rss;

import ani.rss.task.UpdateTrackersTask;
import ani.rss.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        try {
            UpdateUtil.manageWindows();
            ConfigUtil.load();
            AniUtil.load();
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            MenuUtil.start(args);
            UpdateTrackersTask.start();
            ServerUtil.create(args).start();
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
    }

}
