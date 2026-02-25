package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 下载项
 */
@Data
@Accessors(chain = true)
@Schema(description = "下载项")
public class Item implements Serializable {
    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 重命名
     */
    @Schema(description = "重命名")
    private String reName;

    /**
     * 种子
     */
    @Schema(description = "种子")
    private String torrent;

    /**
     * infoHash
     */
    @Schema(description = "infoHash")
    private String infoHash;

    /**
     * 集数
     */
    @Schema(description = "集数")
    private Double episode;

    /**
     * 大小
     */
    @Schema(description = "大小")
    private String size;

    /**
     * 大小
     */
    @Schema(description = "大小")
    private Long length;

    /**
     * 本地已存在
     */
    @Schema(description = "本地已存在")
    private Boolean local;

    /**
     * 主 rss
     */
    @Schema(description = "主 rss")
    private Boolean master;

    /**
     * 字幕组
     */
    @Schema(description = "字幕组")
    private String subgroup;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    private Date pubDate;
}
