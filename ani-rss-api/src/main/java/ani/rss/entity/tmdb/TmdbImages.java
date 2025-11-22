package ani.rss.entity.tmdb;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 图片列表
 */
@Data
@Accessors(chain = true)
public class TmdbImages implements Serializable {
    private String id;

    /**
     * logo
     */
    private List<TmdbImage> logos;

    /**
     * 封面
     */
    private List<TmdbImage> posters;

    /**
     * 背景
     */
    private List<TmdbImage> backdrops;
}
