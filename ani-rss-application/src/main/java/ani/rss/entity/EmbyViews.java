package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Emby 媒体库
 */
@Data
@Accessors(chain = true)
@Schema(description = "Emby 媒体库")
public class EmbyViews implements Serializable {
    @Schema(description = "id")
    @SerializedName(value = "id", alternate = "Id")
    private String id;
    @Schema(description = "名称")
    @SerializedName(value = "name", alternate = "Name")
    private String name;
}
