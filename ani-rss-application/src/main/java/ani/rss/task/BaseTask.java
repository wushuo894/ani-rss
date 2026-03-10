package ani.rss.task;

import cn.hutool.log.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface BaseTask extends BiConsumer<String, AtomicBoolean> {

    Log log = Log.get();

    default void run(String threadName, AtomicBoolean loop) {
        Thread.currentThread().setName(threadName);

        log.info("{} 任务正在运行", threadName);
        while (loop.get()) {
            accept(threadName, loop);
        }
        log.info("{} 任务已停止", threadName);
    }

    /**
     * @param threadName 线程名
     * @param loop       原子化布尔 用以控制循环
     */
    @Override
    void accept(String threadName, AtomicBoolean loop);
}
