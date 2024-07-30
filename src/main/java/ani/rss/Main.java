package ani.rss;

import ani.rss.util.*;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
        ConfigUtil.load();
        AniUtil.load();
        ThreadUtil.execute(() -> ServerUtil.create().start());
        TaskUtil.start();
        String version = MavenUtil.getVersion();
        log.info("version {}", version);
    }
}
