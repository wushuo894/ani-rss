package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Login implements Serializable {
    private String username;
    private String password;
    private String ip;
    private String key;
    private Boolean verifyIp;
}
