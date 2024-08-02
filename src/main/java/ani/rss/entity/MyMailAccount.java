package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class MyMailAccount implements Serializable {
    private String host;
    private Integer port;
    private String from;
    private String pass;
    private Boolean sslEnable;
}
