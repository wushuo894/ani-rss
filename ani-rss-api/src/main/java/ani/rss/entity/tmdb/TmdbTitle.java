package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 标题
 */
@Data
@Accessors(chain = true)
public class TmdbTitle implements Serializable {
    private String title;

    private String type;

    @SerializedName(value = "iso31661", alternate = "iso_3166_1")
    private String iso31661;
}
