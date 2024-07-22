package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Item {
    private String title;

    private String reName;

    private String torrent;

    private Integer length;

    private Integer collect;
}
