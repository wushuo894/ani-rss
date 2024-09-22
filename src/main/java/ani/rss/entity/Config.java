package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

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
     * qb添加下载时修改任务标题
     */
    private Boolean qbRenameTitle;

    /**
     * qb下载时，使用qb自身的保存路径配置(未下载完成的使用临时目录，复制种子文件)
     */
    private Boolean qbUseDownloadPath;

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
     * 间隔/分钟
     */
    private Integer sleep;

    /**
     * 重命名间隔(分钟)
     */
    private Integer renameSleep;

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
     * 自动删除已完成任务
     */
    private Boolean delete;

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
     * 登录信息
     */
    private Login login;

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

}
