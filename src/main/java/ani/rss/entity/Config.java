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
}
