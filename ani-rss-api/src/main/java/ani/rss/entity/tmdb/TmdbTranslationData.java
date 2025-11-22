package ani.rss.entity.tmdb;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 翻译
 */
@Data
@Accessors(chain = true)
public class TmdbTranslationData implements Serializable {
    private String name;

    private String overview;

    private String homepage;

    private String tagline;
}
