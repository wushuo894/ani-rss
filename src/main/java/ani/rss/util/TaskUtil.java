package ani.rss.util;

import ani.rss.task.RenameTask;
import ani.rss.task.RssTask;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TaskUtil {
    public static final AtomicBoolean LOOP = new AtomicBoolean(false);
    public static final List<Thread> THREADS = new Vector<>();

    public static synchronized void stop() {
        LOOP.set(false);
        for (Thread thread : THREADS) {
            try {
                // 等待现有任务结束
                while (thread.isAlive()) {
                    thread.interrupt();
                    ThreadUtil.sleep(500);
                }
                thread.join();
            } catch (Exception e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
            }
        }
        THREADS.clear();
    }

    public static synchronized void restart() {
        stop();
        start();
    }

    public static synchronized void start() {
        LOOP.set(true);
        THREADS.add(new RenameTask(LOOP));
        THREADS.add(new RssTask(LOOP));
        for (Thread thread : THREADS) {
            thread.start();
        }
    }
}
