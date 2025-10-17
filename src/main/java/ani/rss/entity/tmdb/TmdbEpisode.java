package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 集
 */
@Data
@Accessors(chain = true)
public class TmdbEpisode implements Serializable {
    private Integer id;

    @SerializedName(value = "airDate", alternate = "air_date")
    private Date airDate;

    @SerializedName(value = "episodeNumber", alternate = "episode_number")
    private Integer episodeNumber;

    @SerializedName(value = "episodeType", alternate = "episode_type")
    private String episodeType;

    private String name;

    private String overview;

    private String runtime;

    @SerializedName(value = "seasonNumber", alternate = "season_number")
    private Integer seasonNumber;

    @SerializedName(value = "showId", alternate = "show_id")
    private Integer showId;

    @SerializedName(value = "stillPath", alternate = "still_path")
    private String stillPath;

    @SerializedName(value = "voteAverage", alternate = "vote_average")
    private Double voteAverage;

    private Integer order;
}
