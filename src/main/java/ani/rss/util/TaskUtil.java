package ani.rss.util;

import ani.rss.action.AniAction;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TaskUtil {
    public static final String id = "ani-rss";
    public static String currentCron = "";

    public static synchronized void start() {
        Config config = ConfigUtil.getCONFIG();
        Integer sleep = config.getSleep();
        String cron = StrFormatter.format("*/{} * * * *", sleep);
        // 定时未发生改变
        if (StrUtil.isNotBlank(currentCron) && currentCron.equals(cron)) {
            return;
        }
        currentCron = cron;
        Scheduler scheduler = CronUtil.getScheduler();
        if (scheduler.isStarted()) {
            scheduler.stop(true);
        }
        scheduler.schedule(id, currentCron, new Runnable() {
            @Override
            public synchronized void run() {
                if (!TorrentUtil.login()) {
                    return;
                }
                List<Ani> aniList = ObjectUtil.clone(AniAction.getAniList());
                for (Ani ani : aniList) {
                    try {
                        List<Item> items = AniUtil.getItems(ani);
                        TorrentUtil.downloadAni(ani, items);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error("{} {}", ani.getTitle(), message);
                        log.debug(e.getMessage(), e);
                    }
                    // 避免短时间频繁请求导致流控
                    ThreadUtil.sleep(500);
                }
            }
        });
        scheduler.start();
        log.debug("加载定时任务,当前设置 {}", currentCron);
    }
}
