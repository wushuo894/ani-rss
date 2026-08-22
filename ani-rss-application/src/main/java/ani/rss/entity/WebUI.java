package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class WebUI implements Serializable {
    private String owner;
    private String repo;
    private String version;
    private String filename;
}
