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
     * infoHash
     */
    private String infoHash;

    /**
     * 集数
     */
    private Double episode;

    /**
     * 大小
     */
    private String size;

    /**
     * 本地已存在
     */
    private Boolean local;
}
