package ani.rss.entity.tmdb;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 类型
 */
@Data
@Accessors(chain = true)
public class TmdbGenres implements Serializable {
    private Integer id;
    private String name;
}
