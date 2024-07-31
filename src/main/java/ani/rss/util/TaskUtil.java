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
    public static AtomicBoolean loop = new AtomicBoolean(false);
    public static List<Thread> threads = new Vector<>();

    public static synchronized void stop() {
        loop.set(false);
        for (Thread thread : threads) {
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
        threads.clear();
    }

    public static synchronized void restart() {
        stop();
        start();
    }

    public static synchronized void start() {
        loop.set(true);
        threads.add(new RenameTask(loop));
        threads.add(new RssTask(loop));
        for (Thread thread : threads) {
            thread.start();
        }
    }
}
