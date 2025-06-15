package ani.rss.entity;

import ani.rss.util.ConfigUtil;
import cn.hutool.core.lang.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订阅
 */
@Data
@Accessors(chain = true)
public class Ani implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * RSS URL
     */
    private String url;

    private Boolean exists;

    /**
     * 备用rss
     */
    private List<StandbyRss> standbyRssList;

    /**
     * 标题
     */
    private String title;

    /**
     * 日文标题 来源于BGM
     */
    private String jpTitle;

    /**
     * 剧集偏移
     */
    private Integer offset;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月
     */
    private Integer month;

    /**
     * 日
     */
    private Integer date;

    /**
     * 星期 1表示周日，2表示周一
     */
    private Integer week;

    /**
     * 季度
     */
    private Integer season;

    /**
     * 封面本地保存位置
     */
    private String cover;

    /**
     * 图片 https://
     */
    private String image;

    /**
     * 字幕组
     */
    private String subgroup;

    /**
     * 匹配
     */
    private List<String> match;

    /**
     * 排除
     */
    private List<String> exclude;

    /**
     * 是否启用全局排除
     */
    private Boolean globalExclude;

    /**
     * 剧场版 or OVA
     */
    private Boolean ova;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 启用
     */
    private Boolean enable;

    /**
     * 当前集数
     */
    private Integer currentEpisodeNumber;

    /**
     * 总集数
     */
    private Integer totalEpisodeNumber;

    private String themoviedbName;

    private String type;

    private String bgmUrl;

    /**
     * 自定义下载位置
     */
    private Boolean customDownloadPath;

    /**
     * 自定义下载位置
     */
    private String downloadPath;

    /**
     * 评分
     */
    private Double score;

    /**
     * 自定义集数获取规则
     */
    private Boolean customEpisode;

    /**
     * 自定义集数获取规则
     */
    private String customEpisodeStr;

    /**
     * 自定义集数获取规则 groupIndex
     */
    private Integer customEpisodeGroupIndex;

    /**
     * 遗漏检测
     */
    private Boolean omit;

    /**
     * 只下载最新集
     */
    private Boolean downloadNew;

    /**
     * 不进行下载的集
     */
    private List<Double> notDownload;

    /**
     * tmdb 相关信息
     */
    private Tmdb tmdb;

    /**
     * 自动上传
     *
     * @return
     */
    private Boolean upload;

    /**
     * 摸鱼
     */
    private Boolean procrastinating;

    /**
     * 自定义重命名模版
     */
    private Boolean customRenameTemplateEnable;

    /**
     * 自定义重命名模版
     */
    private String customRenameTemplate;

    /**
     * 上次下载完成时间
     */
    private Long lastDownloadTime;

    /**
     * 消息通知
     */
    private Boolean message;

    public static Ani bulidAni() {
        Ani newAni = new Ani();
        Config config = ConfigUtil.CONFIG;
        return newAni
                .setId(UUID.fastUUID().toString())
                .setStandbyRssList(new ArrayList<>())
                .setOffset(0)
                .setYear(1970)
                .setMonth(1)
                .setDate(1)
                .setEnable(true)
                .setOva(false)
                .setScore(0.0)
                .setLastDownloadTime(0L)
                .setImage("")
                .setThemoviedbName("")
                .setCustomDownloadPath(false)
                .setDownloadPath("")
                .setGlobalExclude(false)
                .setCurrentEpisodeNumber(0)
                .setTotalEpisodeNumber(0)
                .setMatch(List.of())
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setBgmUrl("")
                .setSubgroup("")
                .setCustomEpisode(config.getCustomEpisode())
                .setCustomEpisodeStr(config.getCustomEpisodeStr())
                .setCustomEpisodeGroupIndex(config.getCustomEpisodeGroupIndex())
                .setOmit(true)
                .setDownloadNew(false)
                .setNotDownload(new ArrayList<>())
                .setTmdb(
                        new Tmdb()
                                .setId("")
                                .setName("")
                                .setDate(new Date())
                )
                .setUpload(config.getUpload())
                .setProcrastinating(true)
                .setCustomRenameTemplate(config.getRenameTemplate())
                .setCustomRenameTemplateEnable(false)
                .setMessage(true);
    }

    @Data
    @Accessors(chain = true)
    public static class StandbyRss implements Serializable {
        /**
         * 字幕组
         */
        private String label;
        /**
         * url
         */
        private String url;
        /**
         * 剧集偏移
         */
        private Integer offset;
    }
}
