package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Bgm番剧信息")
public class BgmInfo implements Serializable {
    private String id;

    private String url;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 中文名称
     */
    @SerializedName(value = "nameCn", alternate = "name_cn")
    @Schema(description = "中文名称")
    private String nameCn;

    /**
     * 集数
     */
    @Schema(description = "集数")
    private Integer eps;

    /**
     * 时间
     */
    @Schema(description = "时间")
    private Date date;

    /**
     * 图片
     */
    @Schema(description = "图片")
    private Images images;

    /**
     * 季度
     */
    @Schema(description = "季度")
    private Integer season;

    /**
     * 平台  OVA/剧场版
     */
    @Schema(description = "平台 OVA/剧场版")
    private String platform;

    private List<Tag> tags;

    private Rating rating;

    /**
     * 封面图片
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "封面图片")
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
    @Schema(description = "标签")
    public static class Tag implements Serializable {
        @Schema(description = "标签名")
        private String name;

        @Schema(description = "计数")
        private String count;

        @SerializedName(value = "totalCont", alternate = "total_cont")
        @Schema(description = "总计数")
        private String totalCont;
    }

    /**
     * 评分
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "评分")
    public static class Rating implements Serializable {
        /**
         * 级别
         */
        @Schema(description = "级别")
        private Integer rank;

        /**
         * 评分
         */
        @Schema(description = "评分")
        private Double score;

        /**
         * 评分数
         */
        @Schema(description = "评分数")
        private Integer total;

        /**
         * 各阶段评分数
         */
        @Schema(description = "各阶段评分数")
        private Map<String, Integer> count;
    }
}
