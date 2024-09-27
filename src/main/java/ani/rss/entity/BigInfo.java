package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class BigInfo {
    private String subjectId;

    /**
     * 名称
     */
    private String nameCn;

    /**
     * 评分
     */
    private double score;

    /**
     * 集数
     */
    private Integer eps;

    /**
     * ova
     */
    private Boolean ova;

    /**
     * 时间
     */
    private LocalDateTime date;
}
