package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bgm番剧信息
 */
@Data
@Accessors(chain = true)
public class BgmInfo implements Serializable {
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 中文名称
     */
    @SerializedName(value = "nameCn", alternate = "name_cn")
    private String nameCn;

    /**
     * 集数
     */
    private Integer eps;

    /**
     * 时间
     */
    private Date date;

    /**
     * 图片
     */
    private Images images;

    /**
     * 季度
     */
    private Integer season;

    /**
     * 平台  OVA/剧场版
     */
    private String platform;

    private List<Tag> tags;

    private Rating rating;

    /**
     * 封面图片
     */
    @Data
    @Accessors(chain = true)
    public static class Images implements Serializable {
        private String small;
        private String grid;
        private String large;
        private String medium;
        private String common;
    }

    /**
     * 标签
     */
    @Data
    @Accessors(chain = true)
    public static class Tag implements Serializable {
        private String name;

        private String count;

        @SerializedName(value = "totalCont", alternate = "total_cont")
        private String totalCont;
    }

    /**
     * 评分
     */
    @Data
    @Accessors(chain = true)
    public static class Rating implements Serializable {
        /**
         * 级别
         */
        private Integer rank;

        /**
         * 评分
         */
        private Double score;

        /**
         * 评分数
         */
        private Integer total;

        /**
         * 各阶段评分数
         */
        private Map<String, Integer> count;
    }
}
