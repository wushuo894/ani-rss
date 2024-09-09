package ani.rss;

import ani.rss.util.*;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        try {
            MenuUtil.start(args);
            ConfigUtil.load();
            AniUtil.load();
            ThreadUtil.execute(() -> ServerUtil.create(args).start());
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

}
