package ani.rss;

import ani.rss.task.UpdateTrackersTask;
import ani.rss.util.*;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static String[] ARGS = new String[]{};

    public static void main(String[] args) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Main.ARGS = ObjectUtil.defaultIfNull(args, new String[]{});
        try {
            UpdateUtil.manageWindows();
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            MenuUtil.start();
            UpdateTrackersTask.start();
            ServerUtil.create().start();
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
    }

}
