package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class About {
    private String version;
    private String latest;
    private Boolean update;
    private String downloadUrl;
    private String markdownBody;
}