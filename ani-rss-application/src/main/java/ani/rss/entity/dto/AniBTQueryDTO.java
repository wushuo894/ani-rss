package ani.rss.entity.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class AniBTQueryDTO implements Serializable {
    private String season;
    private String bgmUrl;
    private String title;
}
