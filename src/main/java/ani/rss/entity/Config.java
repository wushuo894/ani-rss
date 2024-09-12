package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class Config implements Serializable {
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
     * 自动禁用已完结番剧的订阅
     */
    private Boolean autoDisabled;

    /**
     * 自动跳过 xx.5 集数
     */
    private Boolean skip5;

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

    private String webHookMethod;

    private String webHookUrl;

    private String webHookBody;

    private Boolean webHook;

    private Boolean tmdb;

}
