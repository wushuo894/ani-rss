package ani.rss.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ThemoviedbDTO implements Serializable {
    private String tmdbId;
    private String title;
    private Boolean ova;
}
