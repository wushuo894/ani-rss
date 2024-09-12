package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Log implements Serializable {
    private String message;
    private String level;
    private String loggerName;
}
