package ani.rss.task;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.service.DownloadService;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 重命名
 */
@Slf4j
@Component
public class RenameTask implements BaseTask {
    @Resource
    private DownloadService downloadService;

    @Override
    public void accept(String threadName, AtomicBoolean loop) {
        Config config = ConfigUtil.CONFIG;
        int renameSleepSeconds = config.getRenameSleepSeconds();

        if (!TorrentUtil.login()) {
            ThreadUtil.sleep(renameSleepSeconds * 1000L);
            return;
        }
        try {
            List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                if (!loop.get()) {
                    return;
                }
                Boolean deleteStandbyRSSOnly = config.getDeleteStandbyRSSOnly();
                try {
                    TorrentUtil.rename(torrentsInfo);
                    downloadService.notification(torrentsInfo);
                    if (deleteStandbyRSSOnly) {
                        continue;
                    }
                    TorrentUtil.delete(torrentsInfo);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
        }
        ThreadUtil.sleep(renameSleepSeconds * 1000L);
    }
}
