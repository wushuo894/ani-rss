package ani.rss.entity;

import ani.rss.enums.BgmTokenTypeEnum;
import ani.rss.enums.SortTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 设置
 */
@Data
@Accessors(chain = true)
@Schema(description = "设置")
public class Config implements Serializable {
    /**
     * Mikan Host
     */
    @Schema(description = "Mikan Host")
    private String mikanHost;

    /**
     * tmdbApi
     */
    @Schema(description = "TMDB API")
    private String tmdbApi;

    /**
     * tmdbApiKey
     */
    @Schema(description = "TMDB API Key")
    private String tmdbApiKey;

    /**
     * 仅获取动漫
     */
    @Schema(description = "仅获取动漫")
    private Boolean tmdbAnime;

    /**
     * 下载工具
     */
    @Schema(description = "下载工具类型")
    private String downloadToolType;

    /**
     * 下载重试次数
     */
    @Schema(description = "下载重试次数")
    private Integer downloadRetry;

    /**
     * 下载工具 地址
     */
    @Schema(description = "下载工具地址")
    private String downloadToolHost;

    /**
     * 下载工具 用户名
     */
    @Schema(description = "下载工具用户名")
    private String downloadToolUsername;

    /**
     * 下载工具 密码
     */
    @Schema(description = "下载工具密码")
    private String downloadToolPassword;

    /**
     * qb下载时，使用qb自身的保存路径配置(未下载完成的使用临时目录，复制种子文件)
     */
    @Schema(description = "使用 qb 自身保存路径")
    private Boolean qbUseDownloadPath;

    /**
     * 分享率
     */
    @Schema(description = "分享率")
    private Integer ratioLimit;

    /**
     * 总做种时长
     */
    @Schema(description = "总做种时长")
    private Integer seedingTimeLimit;

    /**
     * 非活跃时长
     */
    @Schema(description = "非活跃时长")
    private Integer inactiveSeedingTimeLimit;

    /**
     * 下载路径
     */
    @Schema(description = "下载路径模版")
    private String downloadPathTemplate;

    /**
     * 剧场版下载路径
     */
    @Schema(description = "剧场版下载路径模版")
    private String ovaDownloadPathTemplate;

    /**
     * 自定义标签
     */
    @Schema(description = "自定义标签")
    private List<String> customTags;

    /*
     * 优先保留开关
     */
    @Schema(description = "优先保留开关")
    private Boolean priorityKeywordsEnable;

    /**
     * 优先保留关键词列表
     */
    @Schema(description = "优先保留关键词列表")
    private List<String> priorityKeywords;

    /**
     * 延迟下载
     */
    @Schema(description = "延迟下载(分钟)")
    private Integer delayedDownload;

    /**
     * 显示评分
     */
    @Schema(description = "显示评分")
    private Boolean scoreShow;

    /**
     * RSS 间隔(分钟)
     */
    @Schema(description = "RSS 间隔(分钟)")
    private Integer rssSleepMinutes;

    /**
     * 重命名间隔(秒)
     */
    @Schema(description = "重命名间隔(秒)")
    private Integer renameSleepSeconds;

    /**
     * 自动重命名
     */
    @Schema(description = "自动重命名")
    private Boolean rename;

    /**
     * rss开关
     */
    @Schema(description = "RSS 开关")
    private Boolean rss;

    /**
     * rss 超时时间 秒
     */
    @Schema(description = "RSS 超时时间(秒)")
    private Integer rssTimeout;

    /**
     * 文件已下载自动跳过
     */
    @Schema(description = "文件已下载自动跳过")
    private Boolean fileExist;

    /**
     * 等待做种完毕
     */
    @Schema(description = "等待做种完毕")
    private Boolean awaitStalledUP;

    /**
     * 自动删除已完成任务
     */
    @Schema(description = "自动删除已完成任务")
    private Boolean delete;

    /**
     * 仅在主RSS更新后删除备用RSS
     */
    @Schema(description = "主RSS更新后删除备用RSS")
    private Boolean deleteStandbyRSSOnly;

    /**
     * 自动推断剧集偏移
     */
    @Schema(description = "自动推断剧集偏移")
    private Boolean offset;

    /**
     * 获取标题时带上年份
     */
    @Schema(description = "获取标题时带上年份")
    private Boolean titleYear;

    /**
     * 自动禁用已完结番剧的订阅
     */
    @Schema(description = "自动禁用已完结番剧订阅")
    private Boolean autoDisabled;

    /**
     * 自动跳过 x.5 集数
     */
    @Schema(description = "自动跳过 x.5 集数")
    private Boolean skip5;

    /**
     * 备用RSS
     */
    @Schema(description = "备用RSS")
    private Boolean standbyRss;

    /**
     * 多字幕组共存模式
     */
    @Schema(description = "多字幕组共存模式")
    private Boolean coexist;

    /**
     * 最大日志条数
     */
    @Schema(description = "最大日志条数")
    private Integer logsMax;

    /**
     * DEBUG
     */
    @Schema(description = "DEBUG")
    private Boolean debug;

    /**
     * 仅启用主rss摸鱼检测
     */
    @Schema(description = "仅启用主RSS摸鱼检测")
    private Boolean procrastinatingMasterOnly;

    /**
     * 代理是否开启
     */
    @Schema(description = "代理是否开启")
    private Boolean proxy;

    /**
     * 代理host
     */
    @Schema(description = "代理 host")
    private String proxyHost;

    /**
     * 代理端口
     */
    @Schema(description = "代理端口")
    private Integer proxyPort;

    /**
     * 代理用户名
     */
    @Schema(description = "代理用户名")
    private String proxyUsername;

    /**
     * 代理密码
     */
    @Schema(description = "代理密码")
    private String proxyPassword;

    /**
     * 同时下载数量限制
     */
    @Schema(description = "同时下载数量限制")
    private Integer downloadCount;

    /**
     * 登录信息
     */
    @Schema(description = "登录信息")
    private Login login;

    /**
     * 禁止多端登录
     */
    @Schema(description = "禁止多端登录")
    private Boolean multiLoginForbidden;

    /**
     * 登录有效时间/小时
     */
    @Schema(description = "登录有效时间(小时)")
    private Integer loginEffectiveHours;

    /**
     * 全局排除
     */
    @Schema(description = "全局排除")
    private List<String> exclude;

    /**
     * 默认导入全局排除
     */
    @Schema(description = "默认导入全局排除")
    private Boolean importExclude;

    /**
     * 默认启用全局排除
     */
    @Schema(description = "默认启用全局排除")
    private Boolean enabledExclude;

    /**
     * BGM日语标题
     */
    @Schema(description = "BGM日语标题")
    private Boolean bgmJpName;

    /**
     * tmdb
     */
    @Schema(description = "启用 TMDB")
    private Boolean tmdb;

    /**
     * 获取标题时带有tmdbId
     */
    @Schema(description = "标题带 TMDB ID")
    private Boolean tmdbId;

    /**
     * 剧集标题是否支持plex命名方式
     */
    @Schema(description = "Plex 命名方式")
    private Boolean tmdbIdPlexMode;

    /**
     * tmdb 语言
     */
    @Schema(description = "TMDB 语言")
    private String tmdbLanguage;

    /**
     * 获取罗马音
     */
    @Schema(description = "获取罗马音")
    private Boolean tmdbRomaji;

    /**
     * 开启ip白名单
     */
    @Schema(description = "开启 IP 白名单")
    private Boolean ipWhitelist;

    /**
     * ip白名单
     */
    @Schema(description = "IP 白名单")
    private String ipWhitelistStr;

    /**
     * 显示已下载视频列表
     */
    @Schema(description = "显示已下载视频列表")
    private Boolean showPlaylist;

    /**
     * 检测遗漏集数
     */
    @Schema(description = "检测遗漏集数")
    private Boolean omit;

    /**
     * bgmTokenType
     * <p>
     * INPUT or AUTO
     */
    @Schema(description = "BGM Token 类型")
    private BgmTokenTypeEnum bgmTokenType;

    /**
     * bgmToken
     */
    @Schema(description = "BGM Token")
    private String bgmToken;

    /**
     * bgmAppID
     */
    @Schema(description = "BGM App ID")
    private String bgmAppID;

    /**
     * bgmAppID
     */
    @Schema(description = "BGM App Secret")
    private String bgmAppSecret;

    /**
     * bgmRefreshToken
     */
    @Schema(description = "BGM Refresh Token")
    private String bgmRefreshToken;

    /**
     * bgmRedirectUri
     */
    @Schema(description = "BGM Redirect URI")
    private String bgmRedirectUri;

    /**
     * api key
     */
    @Schema(description = "API Key")
    private String apiKey;

    /**
     * 按星期展示
     */
    @Schema(description = "按星期展示")
    private Boolean weekShow;

    /**
     * 只下载最新集
     */
    @Schema(description = "只下载最新集")
    private Boolean downloadNew;

    /**
     * 仅允许内网ip访问
     */
    @Schema(description = "仅允许内网 IP 访问")
    private Boolean innerIP;

    /**
     * 重命名模版
     */
    @Schema(description = "重命名模版")
    private String renameTemplate;

    /**
     * 重命名时剔除 年份 如 (2024)
     */
    @Schema(description = "重命名剔除年份")
    private Boolean renameDelYear;

    /**
     * 重命名时剔除 tmdbId [tmdbid=242143]
     */
    @Schema(description = "重命名剔除 TMDB ID")
    private Boolean renameDelTmdbId;

    /**
     * 校验登录IP
     */
    @Schema(description = "校验登录 IP")
    private Boolean verifyLoginIp;

    /**
     * 自动更新 trackers
     */
    @Schema(description = "自动更新 trackers")
    private Boolean autoTrackersUpdate;

    /**
     * Trackers更新地址
     */
    @Schema(description = "Trackers 更新地址")
    private String trackersUpdateUrls;

    /**
     * 消息模版
     */
    @Schema(description = "消息模版")
    private String notificationTemplate;

    /**
     * 自动更新
     */
    @Schema(description = "自动更新")
    private Boolean autoUpdate;

    /**
     * 版本
     */
    @Schema(description = "版本")
    private String version;

    /**
     * 获取BGM封面图片质量
     */
    @Schema(description = "BGM 封面图片质量")
    private String bgmImage;

    /**
     * 自定义CSS
     */
    @Schema(description = "自定义 CSS")
    private String customCss;

    /**
     * 自定义JS
     */
    @Schema(description = "自定义 JS")
    private String customJs;

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
     * OpenList driver
     */
    @Schema(description = "OpenList Driver")
    private String provider;

    /**
     * 添加行订阅是是否开启自动上传
     */
    @Schema(description = "新增订阅自动上传")
    private Boolean upload;

    /**
     * 上传速度限制
     */
    @Schema(description = "上传速度限制")
    private Long upLimit;

    /**
     * 下载速度限制
     */
    @Schema(description = "下载速度限制")
    private Long dlLimit;

    /**
     * 捐赠过期时间
     */
    @Schema(description = "捐赠过期时间")
    private Long expirationTime;

    /**
     * 爱发电订单号
     */
    @Schema(description = "爱发电订单号")
    private String outTradeNo;

    /**
     * 捐赠或试用是否过期
     */
    @Schema(description = "捐赠或试用是否过期")
    private Boolean verifyExpirationTime;

    /**
     * 试用
     */
    @Schema(description = "试用")
    private Boolean tryOut;

    /**
     * 摸鱼
     */
    @Schema(description = "摸鱼")
    private Boolean procrastinating;

    /**
     * 摸鱼天数
     */
    @Schema(description = "摸鱼天数")
    private Integer procrastinatingDay;

    /**
     * github 加速
     */
    @Schema(description = "GitHub 加速")
    private String github;

    /**
     * 自定义github加速
     */
    @Schema(description = "自定义 GitHub 加速")
    private Boolean customGithub;

    /**
     * 自定义github加速网址
     */
    @Schema(description = "自定义 GitHub 加速地址")
    private String customGithubUrl;

    /**
     * github Token
     */
    @Schema(description = "GitHub Token")
    private String githubToken;

    /**
     * 开启 OpenList 列表刷新
     */
    @Schema(description = "开启 OpenList 列表刷新")
    private Boolean alistRefresh;

    /**
     * OpenList 刷新延迟
     */
    @Schema(description = "OpenList 刷新延迟")
    private Long alistRefreshDelayed;

    /**
     * 自动更新总集数信息
     */
    @Schema(description = "自动更新总集数信息")
    private Boolean updateTotalEpisodeNumber;

    /**
     * 强制更新总集数信息
     */
    @Schema(description = "强制更新总集数信息")
    private Boolean forceUpdateTotalEpisodeNumber;

    /**
     * OpenList 离线超时 分钟
     */
    @Schema(description = "OpenList 离线超时(分钟)")
    private Integer alistDownloadTimeout;

    /**
     * OpenList 下载重试次数
     */
    @Schema(description = "OpenList 下载重试次数")
    private Long alistDownloadRetryNumber;

    /**
     * 设置备份
     */
    @Schema(description = "设置备份")
    private Boolean configBackup;

    /**
     * 备份天数
     */
    @Schema(description = "备份天数")
    private Integer configBackupDay;

    /**
     * 展示最后更新时间
     */
    @Schema(description = "展示最后更新时间")
    private Boolean showLastDownloadTime;

    /**
     * 番剧完结迁移
     */
    @Schema(description = "番剧完结迁移")
    private Boolean completed;

    /**
     * 番剧完结迁移位置
     */
    @Schema(description = "番剧完结迁移位置")
    private String completedPathTemplate;

    /**
     * 通知
     */
    @Schema(description = "通知配置列表")
    private List<NotificationConfig> notificationConfigList;

    /**
     * 添加订阅时自动复制主rss至备用rss
     */
    @Schema(description = "添加订阅时复制主RSS至备用")
    private Boolean copyMasterToStandby;

    /**
     * 排序方式
     */
    @Schema(description = "排序方式")
    private SortTypeEnum sortType;

    /**
     * 代理列表
     */
    @Schema(description = "代理列表")
    private String proxyList;

    /**
     * 刮削开关
     */
    @Schema(description = "刮削开关")
    private Boolean scrape;

    /**
     * 重名的订阅将允许被替换
     */
    @Schema(description = "重名订阅允许替换")
    private Boolean replace;

    /**
     * 最大文件名长度 不包含后缀 如: .mkv .mp4
     */
    @Schema(description = "最大文件名长度(不含后缀)")
    private Integer maxFileNameLength;

    /**
     * 限制尝试次数
     */
    @Schema(description = "限制尝试次数")
    private Boolean limitLoginAttempts;

    /**
     * 构建信息
     */
    @Schema(description = "构建信息")
    private String buildInfo;
}
