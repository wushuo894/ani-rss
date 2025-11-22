package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 工作室
 */
@Data
@Accessors(chain = true)
public class TmdbNetwork implements Serializable {
    private String id;

    @SerializedName(value = "logoPath", alternate = "logo_path")
    private String logoPath;

    private String name;

    @SerializedName(value = "originCountry", alternate = "origin_country")
    private String originCountry;
}
