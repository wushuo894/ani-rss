package ani.rss.task;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.Config;
import ani.rss.service.AniService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于更新BGM评分
 */
@Slf4j
public class BgmTask extends Thread {

    private final AtomicBoolean loop;

    public BgmTask(AtomicBoolean loop) {
        this.loop = loop;
    }

    @Override
    public void run() {
        super.setName("bgm-task-thread");
        log.info("{} 任务正在运行", getName());
        while (loop.get()) {
            try {
                BgmUtil.refreshToken();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            List<Ani> aniList = AniUtil.ANI_LIST;
            for (Ani ani : aniList) {
                if (!loop.get()) {
                    return;
                }
                Boolean enable = ani.getEnable();
                if (!enable) {
                    continue;
                }
                BgmInfo bgmInfo;
                try {
                    bgmInfo = BgmUtil.getBgmInfo(ani);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    continue;
                }

                double score = Optional.ofNullable(bgmInfo.getRating())
                        .map(BgmInfo.Rating::getScore)
                        .orElse(0.0);
                ani.setScore(score);

                Config config = ConfigUtil.CONFIG;
                Boolean updateTotalEpisodeNumber = config.getUpdateTotalEpisodeNumber();
                Boolean forceUpdateTotalEpisodeNumber = config.getForceUpdateTotalEpisodeNumber();

                if (!updateTotalEpisodeNumber) {
                    // 未开启更新总集数
                    continue;
                }

                AniService.updateTotalEpisodeNumber(ani, bgmInfo, forceUpdateTotalEpisodeNumber);
            }
            AniUtil.sync();

            ThreadUtil.sleep(12, TimeUnit.HOURS);
        }
        log.info("{} 任务已停止", getName());
    }
}
