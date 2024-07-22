package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Item implements Serializable {
    private String title;

    private String reName;

    private String torrent;

    private Integer length;

    private Integer episode;
}
