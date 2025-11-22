package ani.rss.task;

import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class GcTask extends Thread {
    private final AtomicBoolean loop;

    public GcTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    @Override
    public void run() {
        super.setName("gc-task-thread");
        log.info("{} 任务正在运行", getName());
        Config config = ConfigUtil.CONFIG;
        Integer gcSleep = config.getGcSleep();
        if (gcSleep < 1) {
            log.info("定时GC间隔为 {} 分钟, 将不会自动GC", gcSleep);
            return;
        }
        log.info("定时GC间隔为 {} 分钟", gcSleep);
        while (loop.get()) {
            ThreadUtil.sleep(gcSleep * TimeUnit.MINUTES.toMillis(1));
            if (loop.get()) {
                log.debug("定时GC");
                System.gc();
            }
        }
    }

}
