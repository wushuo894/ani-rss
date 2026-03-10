package ani.rss.service;

import ani.rss.task.BgmTask;
import ani.rss.task.RenameTask;
import ani.rss.task.RssTask;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class TaskService {
    public static final AtomicBoolean LOOP = new AtomicBoolean(false);
    public static final List<Thread> THREADS = new Vector<>();

    public synchronized void stop() {
        LOOP.set(false);
        for (Thread thread : THREADS) {
            try {
                // 等待现有任务结束
                while (thread.isAlive()) {
                    thread.interrupt();
                    ThreadUtil.sleep(100);
                }
                thread.join();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        THREADS.clear();
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public synchronized void start() {
        if (LOOP.get() && !THREADS.isEmpty()) {
            log.warn("任务已经在运行中");
            return;
        }
        LOOP.set(true);
        THREADS.add(new RenameTask(LOOP));
        THREADS.add(new RssTask(LOOP));
        THREADS.add(new BgmTask(LOOP));
        for (Thread thread : THREADS) {
            thread.start();
        }
    }
}
