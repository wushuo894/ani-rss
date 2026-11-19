package ani.rss.config;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.date.DateTime;
import wushuo.tmdb.api.entity.Tmdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 订阅默认值工厂，集中初始化新订阅的基础状态和订阅级功能开关。
 * <p>
 * 自定义集数规则、重命名模板和自动上传默认值取自当前全局配置；
 * 标题、RSS 地址、季号等业务信息由调用方继续补充。
 */
public class DefaultAniFactory {
    /**
     * 持有全局配置对象的引用，创建订阅时读取其当前值，而不是缓存一份配置快照。
     */
    private static final Config CONFIG = ConfigUtil.CONFIG;

    /**
     * 创建带有独立 ID 和初始状态的订阅，不保存订阅，也不触发下载。
     *
     * @return 初始化基础字段并继承部分全局默认值的新订阅对象
     */
    public static Ani create() {
        Ani ani = new Ani();
        return ani
                .setId(UUID.randomUUID().toString())
                .setMikanTitle("")
                .setStandbyRssList(new ArrayList<>())
                .setOffset(0)
                .setReleaseDate(new DateTime())
                .setEnable(true)
                .setOva(false)
                .setScore(0.0)
                .setLastDownloadTime(0L)
                .setImage("")
                .setThemoviedbName("")
                .setCustomDownloadPath(false)
                .setCustomDownloadPathTemplate("")
                .setGlobalExclude(false)
                .setCurrentEpisodeNumber(0)
                .setTotalEpisodeNumber(0)
                .setMatch(List.of())
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setBgmUrl("")
                .setSubgroup("")
                .setCustomEpisode(CONFIG.getCustomEpisode())
                .setCustomEpisodeStr(CONFIG.getCustomEpisodeStr())
                .setCustomEpisodeGroupIndex(CONFIG.getCustomEpisodeGroupIndex())
                .setOmit(true)
                .setDownloadNew(false)
                .setNotDownload(new ArrayList<>())
                .setTmdb(
                        new Tmdb()
                                .setId("")
                                .setName("")
                                .setOriginalName("")
                                .setDate(new Date())
                )
                .setUpload(CONFIG.getUpload())
                .setProcrastinating(true)
                .setCustomRenameTemplate(CONFIG.getRenameTemplate())
                .setCustomRenameTemplateEnable(false)
                .setCustomPriorityKeywordsEnable(false)
                .setCustomPriorityKeywords(new ArrayList<>())
                .setMessage(true)
                .setCustomUploadPathTarget("")
                .setCustomUploadEnable(false)
                .setCompleted(true)
                .setCustomCompleted(false)
                .setCustomCompletedPathTemplate("")
                .setCustomTags(new ArrayList<>())
                .setCustomTagsEnable(false);
    }
}
