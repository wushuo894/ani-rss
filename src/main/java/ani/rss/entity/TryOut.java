package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class TryOut implements Serializable {
    private Boolean enable;
    private Boolean renewal;
    private Integer day;
    private String message;
}
