package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class TmdbImage implements Serializable {
    @SerializedName(value = "aspectRatio", alternate = "aspect_ratio")
    private Double aspectRatio;

    private Integer width;

    private Integer height;

    @SerializedName(value = "iso31661", alternate = "iso_3166_1")
    private String iso31661;

    @SerializedName(value = "iso6391", alternate = "iso_639_1")
    private String iso6391;

    @SerializedName(value = "filePath", alternate = "file_path")
    private String filePath;

    @SerializedName(value = "voteAverage", alternate = "vote_average")
    private Double voteAverage;

    @SerializedName(value = "voteCount", alternate = "vote_count")
    private Integer voteCount;
}
