package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.Serializable;
import java.util.List;

/**
 * 订阅
 */
@Data
@Accessors(chain = true)
@Schema(description = "订阅")
public class Ani implements Serializable {
    /**
     * id
     */
    @Schema(description = "id")
    private String id;

    /**
     * 不在页面显示
     */
    @Schema(description = "不在页面显示")
    private String mikanTitle;

    /**
     * RSS URL
     */
    @Schema(description = "RSS URL")
    private String url;

    private Boolean exists;

    /**
     * 备用rss
     */
    @Schema(description = "备用rss")
    private List<StandbyRss> standbyRssList;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 日语标题 来源于BGM
     */
    @Schema(description = "日语标题 来源于BGM")
    private String jpTitle;

    /**
     * 剧集偏移
     */
    @Schema(description = "剧集偏移")
    private Integer offset;

    /**
     * 年度
     */
    @Schema(description = "年度")
    private Integer year;

    /**
     * 月
     */
    @Schema(description = "月")
    private Integer month;

    /**
     * 日
     */
    @Schema(description = "日")
    private Integer date;

    /**
     * 星期 1表示周日，2表示周一
     */
    @Schema(description = "星期 1表示周日，2表示周一")
    private Integer week;

    /**
     * 季度
     */
    @Schema(description = "季度")
    private Integer season;

    /**
     * 封面本地保存位置
     */
    @Schema(description = "封面本地保存位置")
    private String cover;

    /**
     * 图片 https://
     */
    @Schema(description = "图片 https://")
    private String image;

    /**
     * 字幕组
     */
    @Schema(description = "字幕组")
    private String subgroup;

    /**
     * 匹配
     */
    @Schema(description = "匹配")
    private List<String> match;

    /**
     * 排除
     */
    @Schema(description = "排除")
    private List<String> exclude;

    /**
     * 是否启用全局排除
     */
    @Schema(description = "是否启用全局排除")
    private Boolean globalExclude;

    /**
     * 剧场版 or OVA
     */
    @Schema(description = "剧场版 or OVA")
    private Boolean ova;

    /**
     * 拼音
     */
    @Schema(description = "拼音")
    private String pinyin;

    /**
     * 拼音
     */
    @Schema(description = "拼音首字母")
    private String pinyinInitials;

    /**
     * 启用
     */
    @Schema(description = "启用")
    private Boolean enable;

    /**
     * 当前集数
     */
    @Schema(description = "当前集数")
    private Integer currentEpisodeNumber;

    /**
     * 总集数
     */
    @Schema(description = "总集数")
    private Integer totalEpisodeNumber;

    @Schema(description = "TheMovieDB 名称")
    private String themoviedbName;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "BGM 地址")
    private String bgmUrl;

    /**
     * 自定义下载位置
     */
    @Schema(description = "自定义下载位置")
    private Boolean customDownloadPath;

    /**
     * 自定义下载位置
     */
    @Schema(description = "自定义下载位置路径")
    private String downloadPath;

    /**
     * 评分
     */
    @Schema(description = "评分")
    private Double score;

    /**
     * 自定义集数获取规则
     */
    @Schema(description = "自定义集数获取规则")
    private Boolean customEpisode;

    /**
     * 自定义集数获取规则
     */
    @Schema(description = "自定义集数获取规则表达式")
    private String customEpisodeStr;

    /**
     * 自定义集数获取规则 groupIndex
     */
    @Schema(description = "自定义集数获取规则 groupIndex")
    private Integer customEpisodeGroupIndex;

    /**
     * 遗漏检测
     */
    @Schema(description = "遗漏检测")
    private Boolean omit;

    /**
     * 只下载最新集
     */
    @Schema(description = "只下载最新集")
    private Boolean downloadNew;

    /**
     * 不进行下载的集
     */
    @Schema(description = "不进行下载的集")
    private List<Double> notDownload;

    /**
     * tmdb 相关信息
     */
    @Schema(description = "TMDB 相关信息")
    private Tmdb tmdb;

    /**
     * 自动上传
     */
    @Schema(description = "自动上传")
    private Boolean upload;

    /**
     * 摸鱼
     */
    @Schema(description = "摸鱼")
    private Boolean procrastinating;

    /**
     * 自定义重命名模版
     */
    @Schema(description = "自定义重命名模版开关")
    private Boolean customRenameTemplateEnable;

    /**
     * 自定义重命名模版
     */
    @Schema(description = "自定义重命名模版")
    private String customRenameTemplate;

    /**
     * 自定义优先保留开关
     */
    @Schema(description = "自定义优先保留开关")
    private Boolean customPriorityKeywordsEnable;

    /**
     * 自定义优先保留关键词列表
     */
    @Schema(description = "自定义优先保留关键词列表")
    private List<String> customPriorityKeywords;

    /**
     * 上次下载完成时间
     */
    @Schema(description = "上次下载完成时间")
    private Long lastDownloadTime;

    /**
     * 自定义上传
     */
    @SerializedName(value = "customUploadEnable", alternate = "customAlistPath")
    @Schema(description = "自定义上传开关")
    private Boolean customUploadEnable;

    /**
     * 自定义上传
     */
    @SerializedName(value = "customUploadPathTarget", alternate = "alistPath")
    @Schema(description = "自定义上传目标路径")
    private String customUploadPathTarget;

    /**
     * 消息通知
     */
    @Schema(description = "消息通知")
    private Boolean message;

    /**
     * 完结迁移
     */
    @Schema(description = "完结迁移")
    private Boolean completed;

    /**
     * 自定义完结迁移
     */
    @Schema(description = "自定义完结迁移开关")
    private Boolean customCompleted;

    /**
     * 自定义完结迁移
     */
    @Schema(description = "自定义完结迁移路径模版")
    private String customCompletedPathTemplate;

    /**
     * 自定义标签开关
     */
    @Schema(description = "自定义标签开关")
    private Boolean customTagsEnable;

    /**
     * 单个订阅自定义标签
     */
    @Schema(description = "单个订阅自定义标签")
    private List<String> customTags;


}
