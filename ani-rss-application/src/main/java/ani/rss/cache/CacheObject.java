package ani.rss.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class CacheObject {
    private Long time;
    private String key;
    private Object value;

    /**
     * 是否已过期
     *
     * @return 是/否
     */
    public Boolean expires() {
        return time >= 1 && new Date().getTime() >= time;
    }
}
