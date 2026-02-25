package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * mikan 番剧信息
 */
@Data
@Accessors(chain = true)
@Schema(description = "mikan 番剧信息")
public class MikanInfo implements Serializable {

    /**
     * 番剧 id
     */
    @Schema(description = "番剧 id")
    private String bangumiId;

    /**
     * 封面
     */
    @Schema(description = "封面")
    private String cover;

    /**
     * mikan url
     */
    @Schema(description = "mikan url")
    private String url;

    /**
     * 已存在
     */
    @Schema(description = "已存在")
    private Boolean exists;

    /**
     * 评分
     */
    @Schema(description = "评分")
    private Double score;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * BGM
     */
    @Schema(description = "BGM")
    private String bgmUrl;

    /**
     * 字幕组
     */
    @Schema(description = "字幕组")
    private List<Mikan.Group> groups;
}
