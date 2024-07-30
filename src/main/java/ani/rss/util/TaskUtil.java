package ani.rss.util;

import ani.rss.action.AniAction;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskUtil {
    public static Integer currentSleep = 0;
    public static AtomicBoolean loop = new AtomicBoolean(false);
    public static Thread thread = null;

    public static synchronized void start() {
        Config config = ConfigUtil.getCONFIG();
        Integer sleep = config.getSleep();
        // 定时未发生改变
        if (sleep < 1 || sleep.equals(currentSleep)) {
            return;
        }

        if (Objects.nonNull(thread)) {
            try {
                loop.set(false);
                // 等待现有任务结束
                while (thread.isAlive()) {
                    thread.interrupt();
                    ThreadUtil.sleep(500);
                }
                thread.join();
            } catch (Exception e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
            }
        }
        currentSleep = sleep;
        loop.set(true);
        thread = new Thread(() -> {
            log.debug("加载定时任务,当前设置间隔为 {} 分钟", currentSleep);
            while (loop.get()) {
                if (!TorrentUtil.login()) {
                    ThreadUtil.sleep(currentSleep, TimeUnit.MINUTES);
                    continue;
                }
                try {
                    List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
                    for (TorrentsInfo torrentsInfo : torrentsInfos) {
                        String name = torrentsInfo.getName();
                        TorrentUtil.rename(torrentsInfo, name);
                        TorrentUtil.delete(torrentsInfo);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                    log.debug(e.getMessage(), e);
                }
                List<Ani> aniList = ObjectUtil.clone(AniAction.getAniList());
                for (Ani ani : aniList) {
                    try {
                        TorrentUtil.downloadAni(ani);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error("{} {}", ani.getTitle(), message);
                        log.debug(e.getMessage(), e);
                    }
                    // 避免短时间频繁请求导致流控
                    ThreadUtil.sleep(500);
                }
                ThreadUtil.sleep(currentSleep, TimeUnit.MINUTES);
            }
            log.debug("任务已停止");
        });
        thread.setName("rss-task-thread");
        thread.start();
    }
}
