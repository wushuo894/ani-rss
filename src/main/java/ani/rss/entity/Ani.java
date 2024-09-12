package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class Ani implements Serializable {
    /**
     * RSS URL
     */
    private String url;

    /**
     * 标题
     */
    private String title;

    /**
     * 剧集偏移
     */
    private Integer offset;

    /**
     * 季度
     */
    private Integer season;

    /**
     * 封面
     */
    private String cover;

    /**
     * 字幕组
     */
    private String subgroup;

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

    private String bangumiId;

    /**
     * 自定义下载位置
     */
    private Boolean customDownloadPath;

    /**
     * 自定义下载位置
     */
    private String downloadPath;

    public static Ani bulidAni() {
        Ani newAni = new Ani();
        newAni
                .setOffset(0)
                .setEnable(true)
                .setOva(false)
                .setThemoviedbName("")
                .setCustomDownloadPath(false)
                .setDownloadPath("")
                .setGlobalExclude(false)
                .setCurrentEpisodeNumber(0)
                .setTotalEpisodeNumber(0)
                .setExclude(List.of("720"));
        return newAni;
    }
}
