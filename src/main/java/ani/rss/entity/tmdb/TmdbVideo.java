package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
public class TmdbVideo implements Serializable {
    private String id;

    private String name;

    private String key;

    private String site;

    private Integer size;

    private String type;

    private Boolean official;

    @SerializedName(value = "publishedAt", alternate = "published_at")
    private Date publishedAt;

    @SerializedName(value = "iso31661", alternate = "iso_3166_1")
    private String iso31661;

    @SerializedName(value = "iso6391", alternate = "iso_639_1")
    private String iso6391;
}
