package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * mikan
 */
@Data
@Accessors(chain = true)
@Schema(description = "mikan")
public class Mikan implements Serializable {

    @Schema(description = "季度信息")
    private List<Season> seasons;

    @Schema(description = "星期信息")
    private List<Week> weeks;

    @Schema(description = "总番剧数")
    private Integer totalItem;

    /**
     * 季度信息
     */
    @Data
    @Accessors(chain = true)
    public static class Season implements Serializable {
        /**
         * 年
         */
        @Schema(description = "年")
        private Integer year;
        /**
         * 季度
         */
        @Schema(description = "季度")
        private String season;
        private String seasonLabel;
        private Boolean select;
    }

    /**
     * 星期信息
     */
    @Data
    @Accessors(chain = true)
    public static class Week implements Serializable {
        /**
         * 星期
         */
        @Schema(description = "星期")
        private String weekLabel;
        /**
         * 番剧
         */
        @Schema(description = "番剧")
        private List<MikanInfo> items;
    }

    /**
     * 字幕组
     */
    @Data
    @Accessors(chain = true)
    public static class Group implements Serializable {
        /**
         * 字幕组 id
         */
        private String subgroupId;
        /**
         * 字幕组名称
         */
        private String label;
        /**
         * rss地址
         */
        private String rss;
        /**
         * BgmUrl
         */
        private String bgmUrl;
        /**
         * 更新日
         */
        private String updateDay;
        /**
         * 资源项
         */
        private List<Item> items;

        private GroupRegex groupRegex;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "资源项")
    public static class Item implements Serializable {
        @Schema(description = "标题")
        private String title;
        @Schema(description = "磁力链接")
        private String magnet;
        @Schema(description = "大小")
        private Long size;
        private String formatSize;
        @Schema(description = "创建时间")
        private Date createdAt;
        @Schema(description = "种子地址")
        private String torrent;
    }
}
