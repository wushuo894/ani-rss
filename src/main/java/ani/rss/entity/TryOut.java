package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TryOut {
    private Boolean enable;
    private Boolean renewal;
    private Integer day;
    private String message;
}
