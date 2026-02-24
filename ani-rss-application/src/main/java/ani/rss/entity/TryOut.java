package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@Schema(description = "试用配置")
public class TryOut implements Serializable {
    @Schema(description = "启用")
    private Boolean enable;
    @Schema(description = "续期")
    private Boolean renewal;
    @Schema(description = "天数")
    private Integer day;
    @Schema(description = "消息")
    private String message;
}
