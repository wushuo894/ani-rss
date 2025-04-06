package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Emby 媒体库
 */
@Data
@Accessors(chain = true)
public class EmbyViews implements Serializable {
    private String id;
    private String name;
}
