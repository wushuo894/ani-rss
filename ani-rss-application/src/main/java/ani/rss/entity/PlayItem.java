package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 视频列表
 */
@Data
@Accessors(chain = true)
@Schema(description = "视频列表")
public class PlayItem implements Serializable {

    /**
     * 显示标题
     */
    @Schema(description = "显示标题")
    private String title;

    /**
     * 路径+文件名 bash64
     */
    @Schema(description = "路径+文件名 base64")
    private String filename;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String name;

    /**
     * 最后修改日期
     */
    @Schema(description = "最后修改日期")
    private Long lastModify;

    /**
     * 集数
     */
    @Schema(description = "集数")
    private Double episode;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private String formatSize;

    /**
     * 扩展名
     */
    @Schema(description = "扩展名")
    private String extName;

}
