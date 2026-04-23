package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@Schema(description = "AnimeGarden")
public class AnimeGarden implements Serializable {

    @Data
    @Accessors(chain = true)
    @Schema(description = "星期")
    public static class Week implements Serializable {
        @Schema(description = "星期标签")
        private String weekLabel;
        @Schema(description = "番剧列表")
        private List<Subject> subjects;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "番剧")
    public static class Subject implements Serializable {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "名称")
        private String name;
        @Schema(description = "关键词列表")
        private List<String> keywords;
        @Schema(description = "激活时间")
        private Date activedAt;
        @Schema(description = "是否已归档")
        private String isArchived;
        @Schema(description = "星期标签")
        private String weekLabel;
        @Schema(description = "是否已订阅")
        private Boolean exists;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "字幕组")
    public static class Group implements Serializable {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "名称")
        private String name;
        @Schema(description = "最近更新时间")
        private Date lastUpdatedAt;
        @Schema(description = "资源列表")
        private List<Item> items;
        @Schema(description = "RSS地址")
        private String rss;
        @Schema(description = "BGM ID")
        private String bgmId;

        /**
         * Regex
         */
        @Schema(description = "正则表达式列表")
        private List<List<RegexItem>> regexList;
        @Schema(description = "标签集合")
        private Set<String> tags;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "资源项")
    public static class Item implements Serializable {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "提供者")
        private String provider;
        @Schema(description = "提供者ID")
        private String providerId;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "链接")
        private String href;
        @Schema(description = "类型")
        private String type;
        @Schema(description = "磁力链接")
        private String magnet;
        @Schema(description = "大小")
        private Long size;
        @Schema(description = "创建时间")
        private Date createdAt;
        @Schema(description = "获取时间")
        private Date fetchedAt;
        @Schema(description = "番剧ID")
        private String subjectId;
        @Schema(description = "发布者")
        private Publisher publisher;
        @Schema(description = "字幕组")
        private Fansub fansub;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "发布者")
    public static class Publisher implements Serializable {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "名称")
        private String avatar;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "字幕组")
    public static class Fansub implements Serializable {
        @Schema(description = "ID")
        private String id;
        @Schema(description = "名称")
        private String name;
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
