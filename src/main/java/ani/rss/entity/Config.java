package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Config implements Serializable {
    private String host;
    private String username;
    private String password;
    private Boolean rename;
    private Integer sleep;
    private String downloadPath;
    private Boolean fileExist;
}
