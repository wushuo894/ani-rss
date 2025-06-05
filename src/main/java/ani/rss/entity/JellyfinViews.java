package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Jellyfin 媒体库
 */
@Data
@Accessors(chain = true)
public class JellyfinViews implements Serializable {
    private String id;
    private String name;
}
