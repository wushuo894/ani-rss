package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * tmdb
 */
@Data
@Accessors(chain = true)
public class Tmdb implements Serializable {
    /**
     * id
     */
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 原名
     */
    private String originalName;

    /**
     * 日期
     */
    private Date date;

    /**
     * 剧集组id
     */
    private String tmdbGroupId;
}