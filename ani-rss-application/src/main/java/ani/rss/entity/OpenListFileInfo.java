package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(description = "OpenList 文件信息")
public class OpenListFileInfo implements Serializable {
    @Schema(description = "名称")
    private String name;
    @Schema(description = "大小")
    private Long size;

    @SerializedName(value = "isDir", alternate = "is_dir")
    @Schema(description = "是否为目录")
    private Boolean isDir;
    @Schema(description = "修改时间")
    private Date modified;
    @Schema(description = "创建时间")
    private Date created;
    @Schema(description = "路径")
    private String path;
}
