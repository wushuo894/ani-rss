package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@Schema(description = "备用rss")
public class StandbyRss implements Serializable {
    /**
     * 字幕组
     */
    @Schema(description = "字幕组")
    private String label;
    /**
     * url
     */
    @Schema(description = "url")
    private String url;
    /**
     * 剧集偏移
     */
    @Schema(description = "剧集偏移")
    private Integer offset;
}
