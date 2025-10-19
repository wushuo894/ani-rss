package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 翻译
 */
@Data
@Accessors(chain = true)
public class TmdbTranslation implements Serializable {
    @SerializedName(value = "iso31661", alternate = "iso_3166_1")
    private String iso31661;

    @SerializedName(value = "iso6391", alternate = "iso_639_1")
    private String iso6391;

    private String name;

    @SerializedName(value = "englishName", alternate = "english_name")
    private String englishName;

    private TmdbTranslationData data;
}
