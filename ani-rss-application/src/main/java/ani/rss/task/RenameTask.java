package ani.rss.task;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.TorrentsTags;
import ani.rss.service.DownloadService;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
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
        setName("rename-task-thread");
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
                    Boolean deleteStandbyRSSOnly = config.getDeleteStandbyRSSOnly();
                    try {
                        // FFmpeg 开启时：已打 DOWNLOAD_COMPLETE 但未打 FFMPEG_DONE，说明 FFmpeg 尚未处理完，跳过
                        if (BooleanUtil.isTrue(config.getFfmpegEnable())) {
                            List<String> tags = torrentsInfo.getTags();
                            boolean downloadComplete = tags.contains(TorrentsTags.DOWNLOAD_COMPLETE.getValue());
                            boolean ffmpegDone = tags.contains(TorrentsTags.FFMPEG_DONE.getValue());
                            boolean isAniRss = tags.contains(TorrentsTags.ANI_RSS.getValue());
                            if (isAniRss && downloadComplete && !ffmpegDone) {
                                continue;
                            }
                        }
                        TorrentUtil.rename(torrentsInfo);
                        DownloadService.notification(torrentsInfo);
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
        log.info("{} 任务已停止", getName());
    }
}
