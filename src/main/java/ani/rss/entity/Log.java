package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Log {
    private String message;
    private String level;
}
