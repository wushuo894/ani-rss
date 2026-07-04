package ani.rss.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ThemoviedbVO implements Serializable {
    @Schema(description = "TheMovieDB 名称")
    private String themoviedbName;
    @Schema(description = "TMDB 相关信息")
    private Tmdb tmdb;
}
