package ani.rss;

import ani.rss.action.AniAction;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.util.*;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Main {

    public static void main(String[] args) {
        ConfigUtil.load();
        AniUtil.load();
        // 处理旧图片
        for (Ani ani : AniAction.getAniList()) {
            String cover = ani.getCover();
            if (!ReUtil.contains("http(s*)://", cover)) {
                continue;
            }
            cover = AniUtil.saveJpg(cover);
            ani.setCover(cover);
            AniUtil.sync();
        }
        ThreadUtil.execute(() -> ServerUtil.create().start());
        String version = MavenUtil.getVersion();
        log.info("version {}", version);

        ThreadUtil.execute(() -> {
            Config config = ConfigUtil.getCONFIG();
            while (true) {
                Integer sleep = config.getSleep();
                if (!TorrentUtil.login()) {
                    ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
                    continue;
                }
                List<Ani> aniList = ObjectUtil.clone(AniAction.getAniList());
                for (Ani ani : aniList) {
                    try {
                        List<Item> items = AniUtil.getItems(ani);
                        TorrentUtil.downloadAni(ani, items);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error("{} {}", ani.getTitle(), message);
                        log.debug(e.getMessage(), e);
                    }
                    // 避免短时间频繁请求导致流控
                    ThreadUtil.sleep(500);
                }
                ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
            }
        });
    }
}
