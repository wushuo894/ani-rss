package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class StandbyRss implements Serializable {
    /**
     * 字幕组
     */
    private String label;
    /**
     * url
     */
    private String url;
    /**
     * 剧集偏移
     */
    private Integer offset;
}
