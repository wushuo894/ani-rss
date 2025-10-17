package ani.rss.entity.tmdb;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class TmdbSeason implements Serializable {

    private String id;

    @SerializedName(value = "seasonNumber", alternate = "season_number")
    private Integer seasonNumber;

    @SerializedName(value = "voteAverage", alternate = "vote_average")
    private Double voteAverage;

    @SerializedName(value = "airDate", alternate = "air_date")
    private Date airDate;

    private String name;

    private String overview;

    @SerializedName(value = "episodes")
    private List<TmdbEpisode> episodes;

    @SerializedName(value = "posterPath", alternate = "poster_path")
    private String posterPath;

    private Integer order;

}
