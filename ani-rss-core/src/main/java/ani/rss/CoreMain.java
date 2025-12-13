package ani.rss;

import ani.rss.commons.MavenUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoreMain {
    public static void main(String[] args) {
        String version = MavenUtils.getVersion();
        log.info("version: {}", version);
    }
}
