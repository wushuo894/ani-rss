package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
        Config config = ConfigUtil.getCONFIG();
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
                    String name = torrentsInfo.getName();
                    TorrentUtil.rename(torrentsInfo, name);
                    TorrentUtil.delete(torrentsInfo);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
            }
            ThreadUtil.sleep(renameSleep, TimeUnit.MINUTES);
        }
        log.info("{} 任务已停止", getName());
    }
}
