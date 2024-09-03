package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Config implements Serializable {
    /**
     * qBittorrent 地址
     */
    private String host;

    /**
     * qBittorrent 用户名
     */
    private String username;

    /**
     * qBittorrent 密码
     */
    private String password;

    /**
     * qBittorrent 下载路径
     */
    private String downloadPath;

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
     * 根据首字母存放
     */
    private Boolean acronym;

    /**
     * 自动禁用已完结番剧的订阅
     */
    private Boolean autoDisabled;

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
}
