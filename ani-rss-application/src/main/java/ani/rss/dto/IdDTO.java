package ani.rss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class IdDTO implements Serializable {
    @Schema(description = "id")
    private String id;
}
