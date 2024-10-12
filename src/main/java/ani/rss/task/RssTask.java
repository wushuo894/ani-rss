package ani.rss.task;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class RssTask extends Thread {
    public RssTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    private final AtomicBoolean loop;

    @Override
    public void run() {
        super.setName("rss-task-thread");
        Config config = ConfigUtil.CONFIG;
        Integer sleep = config.getSleep();
        log.info("{} 当前设置间隔为 {} 分钟", getName(), sleep);
        while (loop.get()) {
            if (!config.getRss()) {
                log.debug("rss未启用");
                ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
                continue;
            }
            try {
                sync();
                download();
            } catch (Exception e) {
                String message = ExceptionUtil.getMessage(e);
                log.error(message, e);
            }
            ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
        }
        log.info("{} 任务已停止", getName());
    }

    public static final AtomicBoolean download = new AtomicBoolean(false);

    public static void sync() {
        synchronized (download) {
            if (download.get()) {
                throw new RuntimeException("存在未完成任务，请等待...");
            }
            download.set(true);
        }
    }

    public static void download() {
        try {
            if (!TorrentUtil.login()) {
                return;
            }
            Set<String> ids = AniUtil.ANI_LIST
                    .stream()
                    .map(Ani::getId)
                    .collect(Collectors.toSet());
            for (String id : ids) {
                Optional<Ani> first = AniUtil.ANI_LIST
                        .stream()
                        .filter(it -> it.getId().equals(id))
                        .findFirst();
                if (first.isEmpty()) {
                    continue;
                }
                Ani ani = first.get();

                synchronized (AniUtil.ANI_LIST) {
                    if (!AniUtil.ANI_LIST.contains(ani)) {
                        continue;
                    }
                    String title = ani.getTitle();
                    Boolean enable = ani.getEnable();
                    if (!enable) {
                        log.debug("{} 未启用", title);
                        continue;
                    }
                    try {
                        TorrentUtil.downloadAni(ani);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error("{} {}", title, message);
                        log.error(message, e);
                    }
                }
                // 避免短时间频繁请求导致流控
                ThreadUtil.sleep(500);
            }
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
        } finally {
            download.set(false);
        }
    }
}
