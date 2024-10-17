package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 关于
 */
@Data
@Accessors(chain = true)
public class About {
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
     * 下载地址
     */
    private String downloadUrl;
    /**
     * 更新内容
     */
    private String markdownBody;
}