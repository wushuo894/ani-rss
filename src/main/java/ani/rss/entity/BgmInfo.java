package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Bgm番剧信息
 */
@Data
@Accessors(chain = true)
public class BgmInfo implements Serializable {
    private String subjectId;

    /**
     * 名称
     */
    private String name;

    /**
     * 中文名称
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
    private Date date;

    /**
     * 图片
     */
    private String image;

    /**
     * 季度
     */
    private Integer season;
}
