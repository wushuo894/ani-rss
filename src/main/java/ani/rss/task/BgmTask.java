package ani.rss.task;

import ani.rss.entity.Ani;
import ani.rss.entity.BigInfo;
import ani.rss.util.AniUtil;
import ani.rss.util.BgmUtil;
import ani.rss.util.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BgmTask extends Thread {

    public BgmTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    private final AtomicBoolean loop;

    @Override
    public void run() {
        super.setName("bgm-task-thread");
        log.info("{} 任务正在运行", getName());
        while (loop.get()) {
            List<Ani> aniList = ObjectUtil.clone(AniUtil.ANI_LIST);
            for (Ani ani : aniList) {
                Boolean enable = ani.getEnable();
                Integer totalEpisodeNumber = ani.getTotalEpisodeNumber();
                if (enable || totalEpisodeNumber < 1) {
                    try {
                        BigInfo bgmInfo = BgmUtil.getBgmInfo(ani);
                        double score = bgmInfo.getScore();
                        Integer eps = bgmInfo.getEps();
                        if (totalEpisodeNumber < 1) {
                            ani.setTotalEpisodeNumber(eps);
                        }
                        ani.setScore(score);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error(message, e);
                    }
                    ThreadUtil.sleep(1000);
                }
            }
            AniUtil.sync();
            ThreadUtil.sleep(30, TimeUnit.MINUTES);
        }
        log.info("{} 任务已停止", getName());
    }
}
