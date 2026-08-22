package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
public class UpdateInfo implements Serializable {
    @Schema(description = "更新版本号")
    private String latest;

    @Schema(description = "下载地址")
    private String downloadUrl;

    @Schema(description = "是否可更新")
    private Boolean update;

    @Schema(description = "允许自动更新")
    private Boolean autoUpdate;

    @Schema(description = "SHA256")
    private String sha256;

    @Schema(description = "size")
    private Long size;

    @Schema(description = "formatSize")
    private String formatSize;

    /**
     * 更新内容
     */
    @Schema(description = "更新内容")
    private String markdownBody;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    private Date date;
}
