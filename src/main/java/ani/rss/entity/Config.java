package ani.rss.entity;

import ani.rss.enums.MessageEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 设置
 */
@Data
@Accessors(chain = true)
public class Config implements Serializable {
    /**
     * Mikan Host
     */
    private String mikanHost;

    /**
     * 下载工具
     */
    private String download;

    /**
     * 地址
     */
    private String host;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * qb下载时，使用qb自身的保存路径配置(未下载完成的使用临时目录，复制种子文件)
     */
    private Boolean qbUseDownloadPath;

    /**
     * 分享率
     */
    private Integer ratioLimit;

    /**
     * 总做种时长
     */
    private Integer seedingTimeLimit;

    /**
     * 非活跃时长
     */
    private Integer inactiveSeedingTimeLimit;

    /**
     * 下载路径
     */
    private String downloadPath;

    /**
     * 剧场版下载路径
     */
    private String ovaDownloadPath;

    /**
     * 检测是否死种
     */
    private Boolean watchErrorTorrent;

    /**
     * 延迟下载
     */
    private Integer delayedDownload;

    /**
     * 显示评分
     */
    private Boolean scoreShow;

    /**
     * 间隔/分钟
     */
    private Integer sleep;

    /**
     * 重命名间隔(分钟)
     */
    private Double renameSleep;

    /**
     * 自动重命名
     */
    private Boolean rename;

    /**
     * rss开关
     */
    private Boolean rss;

    /**
     * 文件已下载自动跳过
     */
    private Boolean fileExist;

    /**
     * 等待做种完毕
     */
    private Boolean awaitStalledUP;

    /**
     * 自动删除已完成任务
     */
    private Boolean delete;

    /**
     * 仅在主RSS更新后删除备用RSS
     */
    private Boolean deleteBackRSSOnly;

    /**
     * 自动推断剧集偏移
     */
    private Boolean offset;

    /**
     * 获取标题时带上年份
     */
    private Boolean titleYear;

    /**
     * 根据首字母存放
     */
    private Boolean acronym;

    /**
     * 根据季度存放
     */
    private Boolean quarter;

    /**
     * 自动禁用已完结番剧的订阅
     */
    private Boolean autoDisabled;

    /**
     * 自动跳过 xx.5 集数
     */
    private Boolean skip5;

    /**
     * 备用RSS
     */
    private Boolean backRss;

    /**
     * 最大日志条数
     */
    private Integer logsMax;

    /**
     * DEBUG
     */
    private Boolean debug;

    /**
     * 代理是否开启
     */
    private Boolean proxy;

    /**
     * 代理host
     */
    private String proxyHost;

    /**
     * 代理端口
     */
    private Integer proxyPort;

    /**
     * 代理用户名
     */
    private String proxyUsername;

    /**
     * 代理密码
     */
    private String proxyPassword;

    /**
     * 同时下载数量限制
     */
    private Integer downloadCount;

    /**
     * 邮箱是否开启
     */
    private Boolean mail;

    /**
     * 发件人
     */
    private MyMailAccount mailAccount;

    /**
     * 收件人
     */
    private String mailAddressee;

    /**
     * mail 发送图片
     */
    private Boolean mailImage;

    /**
     * 登录信息
     */
    private Login login;

    /**
     * 登录有效时间/小时
     */
    private Integer loginEffectiveHours;

    /**
     * 全局排除
     */
    private List<String> exclude;

    /**
     * 默认导入全局排除
     */
    private Boolean importExclude;

    /**
     * 默认启用全局排除
     */
    private Boolean enabledExclude;

    /**
     * telegram
     */
    private Boolean telegram;

    /**
     * telegram bot token
     */
    private String telegramBotToken;

    /**
     * telegram chat_id
     */
    private String telegramChatId;

    /**
     * telegram Api Host
     */
    private String telegramApiHost;

    /**
     * telegram 发送图片
     */
    private Boolean telegramImage;

    /**
     * webHookMethod
     */
    private String webHookMethod;

    /**
     * webHookUrl
     */
    private String webHookUrl;

    /**
     * webHookBody
     */
    private String webHookBody;

    /**
     * webHook
     */
    private Boolean webHook;

    /**
     * tmdb
     */
    private Boolean tmdb;

    /**
     * 获取标题时带有tmdbId
     */
    private Boolean tmdbId;

    /**
     * 使用台湾翻译
     */
    private Boolean tmdbTw;

    /**
     * 开启ip白名单
     */
    private Boolean ipWhitelist;

    /**
     * ip白名单
     */
    private String ipWhitelistStr;

    /**
     * 季命名方式
     */
    private String seasonName;

    /**
     * 显示已下载视频列表
     */
    private Boolean showPlaylist;

    /**
     * 检测遗漏集数
     */
    private Boolean omit;

    /**
     * BgmToken
     */
    private String bgmToken;

    /**
     * api key
     */
    private String apiKey;

    /**
     * 按星期展示
     */
    private Boolean weekShow;

    /**
     * 只下载最新集
     */
    private Boolean downloadNew;

    /**
     * 仅允许内网ip访问
     */
    private Boolean innerIP;

    /**
     * 重命名模版
     */
    private String renameTemplate;

    /**
     * 重命名时 ${title} 去除 年份 如 (2024)
     */
    private Boolean renameDelYear;

    /**
     * 重命名时 ${title} 去除 tmdbId [tmdbid=242143]
     */
    private Boolean renameDelTmdbId;

    /**
     * 通知类型
     */
    private List<MessageEnum> messageList;

    /**
     * 校验登录IP
     */
    private Boolean verifyLoginIp;

    /**
     * server酱类型：server酱和server酱3
     */
    private String serverChanType;

    /**
     * server酱 sendKey
     */
    private String serverChanSendKey;

    /**
     * server酱3 apiUrl
     */
    private String serverChan3ApiUrl;

    /**
     * server酱 开关
     */
    private Boolean serverChan;

    /**
     * 系统通知
     */
    private Boolean systemMsg;

    /**
     * 自动更新 trackers
     */
    private Boolean autoTrackersUpdate;

    /**
     * Trackers更新地址
     */
    private String trackersUpdateUrls;

    /**
     * 消息模版
     */
    private String messageTemplate;

    /**
     * 自动更新
     */
    private Boolean autoUpdate;

    /**
     * Alist
     */
    private String alistHost;

    /**
     * Alist 令牌
     */
    private String alistToken;

    /**
     * 上传位置
     */
    private String alistPath;

    /**
     * 启用alist
     */
    private Boolean alist;
}
