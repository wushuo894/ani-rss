package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@Schema(description = "AniBT")
public class AniBT implements Serializable {
    @Schema(description = "当前季度")
    private String currentSeason;
    @Schema(description = "请求的季度")
    private String requestedSeason;
    @Schema(description = "可用的季度列表")
    private List<String> availableSeasons;
    @Schema(description = "按星期分类的番剧列表")
    private List<ByWeekday> byWeekday;


    @Data
    @Accessors(chain = true)
    @Schema(description = "按星期分类")
    public static class ByWeekday implements Serializable {
        @Schema(description = "番剧列表")
        private List<Anime> animes;
        @Schema(description = "星期几")
        private Integer weekday;
        @Schema(description = "星期标签")
        private String weekdayLabel;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "番剧信息")
    public static class Anime implements Serializable {
        @Schema(description = "番剧ID")
        private String animeId;
        @Schema(description = "BGM ID")
        private String bgmId;
        @Schema(description = "封面图片")
        private String cover;
        @Schema(description = "评分")
        private Double rating;
        @Schema(description = "标题")
        private Title title;
        @Schema(description = "格式")
        private String format;
        @Schema(description = "是否已订阅")
        private Boolean exists;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "标题信息")
    public static class Title implements Serializable {
        @Schema(description = "中文标题")
        private String chinese;
        @Schema(description = "繁体中文标题")
        private String chineseTraditional;
        @Schema(description = "英文标题")
        private String english;
        @Schema(description = "主要标题")
        private String primary;
        @Schema(description = "罗马音标题")
        private String romaji;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "字幕组信息")
    public static class Group implements Serializable {
        @Schema(description = "BGM ID")
        private String bgmId;
        @Schema(description = "字幕组ID")
        private String groupId;
        @Schema(description = "slug")
        private String slug;
        @Schema(description = "字幕组名称")
        private String name;
        @Schema(description = "状态")
        private String status;
        @Schema(description = "最后更新时间")
        private Long lastUpdatedAt;
        @Schema(description = "资源列表")
        private List<Item> items;
        @Schema(description = "RSS地址")
        private String rss;

        /**
         * Regex
         */
        @Schema(description = "正则表达式列表")
        private List<List<AniBT.RegexItem>> regexList;
        @Schema(description = "标签集合")
        private Set<String> tags;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "资源项")
    public static class Item implements Serializable {
        @Schema(description = "剧集键")
        private String episodeKey;
        @Schema(description = "语言列表")
        private List<String> language;
        @Schema(description = "磁力链接")
        private String magnet;
        @Schema(description = "发布时间")
        private Long publishedAt;
        @Schema(description = "发布ID")
        private String releaseId;
        @Schema(description = "分辨率")
        private String resolution;
        @Schema(description = "字幕")
        private String subtitle;
        @Schema(description = "标题")
        private String title;
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "正则表达式项")
    public static class RegexItem implements Serializable {
        @Schema(description = "标签")
        private String label;
        @Schema(description = "正则表达式")
        private String regex;
    }
}
