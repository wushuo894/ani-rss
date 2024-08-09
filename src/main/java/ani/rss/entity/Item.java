package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Item implements Serializable {
    /**
     * 标题
     */
    private String title;

    /**
     * 重命名
     */
    private String reName;

    /**
     * 种子
     */
    private String torrent;

    /**
     * 集数
     */
    private Integer episode;
}
