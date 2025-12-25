package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 关于
 */
@Data
@Accessors(chain = true)
public class About implements Serializable {
    /**
     * 版本
     */
    private String version;

    /**
     * 最新版本
     */
    private String latest;

    /**
     * 是否需要更新
     */
    private Boolean update;

    /**
     * 是否允许自动更新
     */
    private Boolean autoUpdate;

    /**
     * 下载地址
     */
    private String downloadUrl;

    /**
     * 更新内容
     */
    private String markdownBody;

    /**
     * 发布时间
     */
    private Date date;
}
