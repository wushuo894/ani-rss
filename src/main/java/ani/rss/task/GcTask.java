package ani.rss.task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GcTask implements Runnable {

    @Override
    public void run() {
        log.info("GCTask正在运行");
        System.gc();
    }

}
