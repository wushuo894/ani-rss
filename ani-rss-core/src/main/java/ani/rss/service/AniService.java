package ani.rss.service;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.util.other.BgmUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AniService {

    /**
     * 更新订阅总集数
     *
     * @param ani     订阅
     * @param bgmInfo BGM
     * @param force   强制
     */
    public static void updateTotalEpisodeNumber(Ani ani, BgmInfo bgmInfo, Boolean force) {
        Integer totalEpisodeNumber = ani.getTotalEpisodeNumber();
        if (!force) {
            // 未开启强制更新
            if (totalEpisodeNumber > 0) {
                // 总集数不为 0
                return;
            }
        }

        String title = ani.getTitle();

        // 自动更新总集数信息
        int bgmEp = BgmUtil.getEps(bgmInfo);
        if (bgmEp == totalEpisodeNumber) {
            // 集数未发生改变
            return;
        }

        ani.setTotalEpisodeNumber(totalEpisodeNumber);

        log.info("{} 总集数发生更新: {}", title, totalEpisodeNumber);
    }
}
