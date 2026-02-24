package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 合集
 */
@Data
@Accessors(chain = true)
@Schema(description = "合集")
public class CollectionInfo implements Serializable {
    /**
     * 种子文件 base64
     */
    @Schema(description = "种子文件 base64")
    private String torrent;

    /**
     * 订阅
     */
    @Schema(description = "订阅")
    private Ani ani;

    /**
     * bgm
     */
    @Schema(description = "bgm")
    private BgmInfo bgmInfo;
}
