package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.MessageEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.MessageUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RenameTask extends Thread {

    public RenameTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    private final AtomicBoolean loop;

    @Override
    public void run() {
        super.setName("rename-task-thread");
        Config config = ConfigUtil.CONFIG;
        Integer renameSleep = config.getRenameSleep();

        log.info("{} 当前设置间隔为 {} 分钟", getName(), renameSleep);
        while (loop.get()) {
            if (!TorrentUtil.login()) {
                ThreadUtil.sleep(renameSleep, TimeUnit.MINUTES);
                continue;
            }
            try {
                List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    TorrentUtil.rename(torrentsInfo);

                    String name = torrentsInfo.getName();
                    TorrentsInfo.State state = torrentsInfo.getState();
                    if (!List.of(
                            TorrentsInfo.State.pausedUP.name(),
                            TorrentsInfo.State.stoppedUP.name()
                    ).contains(state.name())) {
                        continue;
                    }
                    String tags = torrentsInfo.getTags();
                    if (List.of(tags.split(",")).contains("下载完成")) {
                        TorrentUtil.delete(torrentsInfo);
                        continue;
                    }
                    Boolean b = TorrentUtil.addTags(torrentsInfo, "下载完成");
                    TorrentUtil.delete(torrentsInfo);
                    if (!b) {
                        continue;
                    }
                    MessageUtil.send(ConfigUtil.CONFIG, null, name + " 下载完成", MessageEnum.DOWNLOAD_END);
                }
            } catch (Exception e) {
                String message = ExceptionUtil.getMessage(e);
                log.error(message);
                log.debug(message, e);
            }
            ThreadUtil.sleep(renameSleep, TimeUnit.MINUTES);
        }
        log.info("{} 任务已停止", getName());
    }
}
