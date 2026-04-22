package ani.rss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class RssToAniDTO implements Serializable {
    @Schema(description = "RSS URL", defaultValue = "https://mikanani.me/RSS/Bangumi?bangumiId=3828&subgroupid=370")
    private String url;

    @Schema(description = "类型 mikan/anibt/other", defaultValue = "mikan")
    private String type;

    @Schema(description = "BGM 地址", defaultValue = "https://bgm.tv/subject/544109")
    private String bgmUrl;

    @Schema(description = "字幕组名")
    private String subgroup;
}
