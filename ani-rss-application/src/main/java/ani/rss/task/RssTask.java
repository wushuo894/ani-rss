package ani.rss.task;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RSS
 */
@Slf4j
@Component
public class RssTask implements BaseTask {
    public static final AtomicBoolean download = new AtomicBoolean(false);

    public static void download(AtomicBoolean loop) {
        DownloadService downloadService = SpringUtil.getBean(DownloadService.class);

        try {
            if (!TorrentUtil.login()) {
                return;
            }
            for (Ani ani : AniUtil.ANI_LIST) {
                if (!loop.get()) {
                    return;
                }

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
                    downloadService.downloadAni(ani);
                } catch (Exception e) {
                    String message = ExceptionUtils.getMessage(e);
                    log.error("{} {}", title, message);
                    log.error(message, e);
                }
                // 避免短时间频繁请求导致流控
                ThreadUtil.sleep(500);
            }
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
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
    public void accept(AtomicBoolean loop) {
        Config config = ConfigUtil.CONFIG;
        Integer sleep = config.getRssSleepMinutes();

        if (!config.getRss()) {
            log.debug("rss未启用");
            ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
            return;
        }

        try {
            sync();
            download(loop);
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
        }
        ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
    }
}
