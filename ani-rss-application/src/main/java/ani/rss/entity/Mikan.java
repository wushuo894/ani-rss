package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

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
    private List<Item> items;

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
        private Boolean select;
    }

    /**
     * 星期信息
     */
    @Data
    @Accessors(chain = true)
    public static class Item implements Serializable {
        /**
         * 星期
         */
        @Schema(description = "星期")
        private String label;
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
         * 更新日
         */
        private String updateDay;
        /**
         * 下载项
         */
        private List<TorrentsInfo> items;
        /**
         * Regex
         */
        private List<List<RegexItem>> regexList;
        private Set<String> tags;
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegexItem implements Serializable {
        private String label;
        private String regex;
    }
}
