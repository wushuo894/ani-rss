package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.TorrentUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 重命名
 */
@Slf4j
public class RenameTask implements Runnable {

    private final AtomicBoolean loop;

    public RenameTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    @Override
    public void run() {
        Config config = ConfigUtil.CONFIG;

        if (!TorrentUtil.login()) {
            log.warn("下载器未登录，任务结束");
            return;
        }
        try {
            List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                if (!loop.get()) {
                    return;
                }
                Boolean deleteBackRSSOnly = config.getDeleteBackRSSOnly();
                try {
                    TorrentUtil.rename(torrentsInfo);
                    TorrentUtil.notification(torrentsInfo);
                    if (deleteBackRSSOnly) {
                        continue;
                    }
                    TorrentUtil.delete(torrentsInfo);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
        }
        log.info("RenameTask已停止");
    }
}
