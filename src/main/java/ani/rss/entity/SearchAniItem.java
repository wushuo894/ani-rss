package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class SearchAniItem implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 不在页面显示
     */
    private String mikanTitle;

    /**
     * 标题
     */
    private String title;

    /**
     * 日语标题 来源于BGM
     */
    private String jpTitle;

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
     * 图片 https://
     */
    private String image;

    /**
     * 字幕组
     */
    private String subgroup;

    /**
     * 剧场版 or OVA
     */
    private Boolean ova;

    /**
     * 拼音
     */
    private String pinyin;

    /**
     * 拼音
     */
    private String pinyinInitials;

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

    private Integer bgmSubjectId;

    /**
     * 评分
     */
    private Double score;

    /**
     * tmdb 相关信息
     */
    private Tmdb tmdb;

    /**
     * 播放列表
     */
    private List<PlayItem> playlist;
}
