package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * mikan 番剧信息
 */
@Data
@Accessors(chain = true)
public class MikanInfo implements Serializable {

    /**
     * 番剧 id
     */
    private String bangumiId;

    /**
     * 封面
     */
    private String cover;

    /**
     * mikan url
     */
    private String url;

    /**
     * 已存在
     */
    private Boolean exists;

    /**
     * 评分
     */
    private Double score;

    /**
     * 标题
     */
    private String title;

    /**
     * BGM
     */
    private String bgmUrl;

    /**
     * 字幕组
     */
    private List<Mikan.Group> groups;
}
