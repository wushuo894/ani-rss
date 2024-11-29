package ani.rss.task;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.util.AniUtil;
import ani.rss.util.BgmUtil;
import ani.rss.util.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于更新BGM评分
 */
@Slf4j
public class BgmTask implements Runnable {

    private final AtomicBoolean loop;

    public BgmTask(AtomicBoolean loop) {
        this.loop = loop;
    }


    @Override
    public void run() {
        try {
            log.info("BgmTask正在运行");
            List<Ani> aniList = AniUtil.ANI_LIST;
            for (Ani ani : aniList) {
                if (!loop.get()) {
                    return;
                }
                String bgmUrl = ani.getBgmUrl();
                if (StrUtil.isBlank(bgmUrl)) {
                    continue;
                }
                Boolean enable = ani.getEnable();
                double score = ani.getScore();
                if (enable || score < 1) {
                    try {
                        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
                        score = bgmInfo.getScore();
                        ani.setScore(score);
                    } catch (Exception e) {
                        String message = ExceptionUtil.getMessage(e);
                        log.error(message, e);
                    }
                    ThreadUtil.sleep(1000);
                }
            }
            AniUtil.sync();
        } finally {
            log.info("BgmTask已结束");
        }
    }
}
