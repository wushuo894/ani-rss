package ani.rss;

import ani.rss.util.*;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        try {
            ConfigUtil.load();
            AniUtil.load();
            ThreadUtil.execute(() -> ServerUtil.create(args).start());
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            MenuUtil.start(args);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

}
