package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 合集
 */
@Data
@Accessors(chain = true)
public class CollectionInfo implements Serializable {
    /**
     * 种子文件 base64
     */
    private String torrent;

    /**
     * 订阅
     */
    private Ani ani;

    /**
     * bgm
     */
    private BgmInfo bgmInfo;
}
