package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 重命名
 */
@Slf4j
public class RenameTask extends Thread {

    private final AtomicBoolean loop;

    public RenameTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    @Override
    public void run() {
        super.setName("rename-task-thread");
        Config config = ConfigUtil.CONFIG;
        int renameSleepSeconds = config.getRenameSleepSeconds();

        log.info("{} 当前设置间隔为 {} 秒", getName(), renameSleepSeconds);
        while (loop.get()) {
            if (!TorrentUtil.login()) {
                ThreadUtil.sleep(renameSleepSeconds * 1000L);
                continue;
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
            ThreadUtil.sleep(renameSleepSeconds * 1000L);
        }
        log.info("{} 任务已停止", getName());
    }
}
