package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.task.BgmTask;
import ani.rss.task.GcTask;
import ani.rss.task.RenameTask;
import ani.rss.task.RssTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskUtil {
    private static final ScheduledExecutorService SERVICE = new ScheduledThreadPoolExecutor(4);
    private static final List<Future<?>> FUTURES = new ArrayList<>();
    private static final AtomicBoolean LOOP = new AtomicBoolean(false);


    public static synchronized void stop() {
        LOOP.set(false);
        FUTURES.forEach(future -> future.cancel(true));
        FUTURES.clear();
    }

    public static synchronized void restart() {
        stop();
        start();
    }

    public static synchronized void start() {
        LOOP.set(true);
        Config config = ConfigUtil.CONFIG;
        if (config.getRss()) {
            log.info("启动定时RSS任务，间隔为{}", config.getSleep());
            FUTURES.add(
                    SERVICE.scheduleAtFixedRate(new RssTask(LOOP), 0, config.getSleep(), TimeUnit.MINUTES)
            );
        }
        if (config.getRename()) {
            log.info("启动定时重命名任务，间隔为{}", config.getRenameSleep());
            // warn: 底层不支持0.5分钟的写法，这里强转可能会导致精度丢失, 为了兼容原先代码保留
            FUTURES.add(
                    SERVICE.scheduleAtFixedRate(new RenameTask(LOOP), 5, (long) (config.getRenameSleep() * 60), TimeUnit.SECONDS)
            );
        }
        log.info("启动定时BGM任务，间隔为12小时");
        FUTURES.add(
                SERVICE.scheduleAtFixedRate(new BgmTask(LOOP), 2, 12 * 60, TimeUnit.MINUTES)
        );
        if (config.getGcSleep() > 1) {
            log.info("启动定时GC，间隔为{}", config.getGcSleep());
            FUTURES.add(
                    SERVICE.scheduleAtFixedRate(new GcTask(), config.getGcSleep(), config.getGcSleep(), TimeUnit.MINUTES)
            );
        }


    }
}
