package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 关于
 */
@Data
@Accessors(chain = true)
@Schema(description = "关于")
@EqualsAndHashCode(callSuper = true)
public class About extends UpdateInfo implements Serializable {
    /**
     * 版本
     */
    @Schema(description = "版本")
    private String version;
}
