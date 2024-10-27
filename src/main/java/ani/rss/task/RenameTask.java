package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
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
        double renameSleep = config.getRenameSleep();
        Boolean deleteBackRSSOnly = config.getDeleteBackRSSOnly();

        log.info("{} 当前设置间隔为 {} 分钟", getName(), renameSleep);
        while (loop.get()) {
            if (!TorrentUtil.login()) {
                ThreadUtil.sleep(renameSleep * TimeUnit.MINUTES.toMillis(1));
                continue;
            }
            try {
                List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    if (!loop.get()) {
                        return;
                    }
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
            ThreadUtil.sleep(renameSleep * TimeUnit.MINUTES.toMillis(1));
        }
        log.info("{} 任务已停止", getName());
    }
}
