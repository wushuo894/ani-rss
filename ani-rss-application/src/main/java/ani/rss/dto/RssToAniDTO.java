package ani.rss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class RssToAniDTO implements Serializable {
    @Schema(description = "RSS URL")
    private String url;

    @Schema(description = "类型 mikan/other")
    private String type;

    @Schema(description = "BGM 地址")
    private String bgmUrl;
}
