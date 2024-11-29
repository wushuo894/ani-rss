package ani.rss.task;

import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * RSS
 */
@Slf4j
public class RssTask implements Runnable {
    public static final AtomicBoolean download = new AtomicBoolean(false);
    private final AtomicBoolean loop;

    public RssTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    public static void download(AtomicBoolean loop) {
        try {
            if (!TorrentUtil.login()) {
                return;
            }
            Set<String> ids = AniUtil.ANI_LIST
                    .stream()
                    .map(Ani::getId)
                    .collect(Collectors.toSet());
            for (String id : ids) {
                if (!loop.get()) {
                    return;
                }
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

    public static void sync() {
        synchronized (download) {
            if (download.get()) {
                throw new RuntimeException("存在未完成任务，请等待...");
            }
            download.set(true);
        }
    }

    @Override
    public void run() {
        try {
            sync();
            download(this.loop);
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
        }
        log.info("RSSTask已停止");
    }
}
