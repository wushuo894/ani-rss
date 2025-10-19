package ani.rss.entity.tmdb;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 翻译
 */
@Data
@Accessors(chain = true)
public class TmdbTranslations implements Serializable {
    private List<TmdbTranslation> translations;
}
