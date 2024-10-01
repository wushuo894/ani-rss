package ani.rss.entity;

import cn.hutool.core.lang.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
     *
     * @deprecated
     */
    private List<String> backRss;

    /**
     * 备用rss
     */
    private List<BackRss> backRssList;

    /**
     * 标题
     */
    private String title;

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
     * 剧场版
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

    public static Ani bulidAni() {
        Ani newAni = new Ani();
        return newAni
                .setId(UUID.fastUUID().toString())
                .setBackRss(new ArrayList<>())
                .setBackRssList(new ArrayList<>())
                .setOffset(0)
                .setYear(1970)
                .setMonth(1)
                .setDate(1)
                .setEnable(true)
                .setOva(false)
                .setScore(0.0)
                .setThemoviedbName("")
                .setCustomDownloadPath(false)
                .setDownloadPath("")
                .setGlobalExclude(false)
                .setCurrentEpisodeNumber(0)
                .setTotalEpisodeNumber(0)
                .setMatch(List.of())
                .setExclude(List.of("720", "\\d{1,2}-\\d{1,2}", "合集"))
                .setBgmUrl("")
                .setSubgroup("")
                .setCustomEpisode(false)
                .setCustomEpisodeStr("\\d{1,2}(\\.5)?")
                .setCustomEpisodeGroupIndex(0)
                .setOmit(true)
                .setDownloadNew(false);
    }

    @Data
    @Accessors(chain = true)
    public static class BackRss {
        private String label;
        private String url;
    }
}
