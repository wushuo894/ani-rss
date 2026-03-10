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
                        // 下载前重命名（种子暂停状态），始终执行
                        TorrentUtil.rename(torrentsInfo);
                        // FFmpeg 开启时：下载完成但转码未完成，等待 FfmpegTask 处理后再继续
                        if (BooleanUtil.isTrue(config.getFfmpegEnable())) {
                            List<String> tags = torrentsInfo.getTags();
                            boolean isAniRss = tags.contains(TorrentsTags.ANI_RSS.getValue());
                            boolean downloadComplete = tags.contains(TorrentsTags.DOWNLOAD_COMPLETE.getValue());
                            boolean ffmpegDone = tags.contains(TorrentsTags.FFMPEG_DONE.getValue());
                            if (isAniRss && downloadComplete && !ffmpegDone) {
                                continue;
                            }
                        }
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
