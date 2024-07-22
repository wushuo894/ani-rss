package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * tmdb剧集组
 */
@Data
@Accessors(chain = true)
public class TmdbGroup implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 剧集组名
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 类型名
     */
    private String typeName;

    /**
     * 集数量
     */
    private String episode_count;

    /**
     * 组数量
     */
    private String group_count;

    /**
     * 描述
     */
    private String description;
}