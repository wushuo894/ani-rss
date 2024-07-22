package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class Ani implements Serializable {
    /**
     * RSS URL
     */
    private String url;

    /**
     * 标题
     */
    private String title;

    /**
     * 剧集偏移
     */
    private Integer off;

    /**
     * 季度
     */
    private Integer season;

    /**
     * 封面
     */
    private String cover;

    /**
     * 排除
     */
    private List<String> exclude;
}
