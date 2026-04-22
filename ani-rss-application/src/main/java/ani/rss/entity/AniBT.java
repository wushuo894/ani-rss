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
    private String currentSeason;
    private String requestedSeason;
    private List<String> availableSeasons;
    private List<ByWeekday> byWeekday;


    @Data
    @Accessors(chain = true)
    public static class ByWeekday implements Serializable {
        private List<Anime> animes;
        private Integer weekday;
        private String weekdayLabel;
    }

    @Data
    @Accessors(chain = true)
    public static class Anime implements Serializable {
        private String animeId;
        private String bgmId;
        private String cover;
        private Double rating;
        private Title title;
        private String format;
        private Boolean exists;
    }

    @Data
    @Accessors(chain = true)
    public static class Title implements Serializable {
        private String chinese;
        private String chineseTraditional;
        private String english;
        private String primary;
        private String romaji;
    }

    @Data
    @Accessors(chain = true)
    public static class Group implements Serializable {
        private String bgmId;
        private String groupId;
        private String slug;
        private String name;
        private String status;
        private Long lastUpdatedAt;
        private List<Item> items;
        private String rss;

        /**
         * Regex
         */
        private List<List<AniBT.RegexItem>> regexList;
        private Set<String> tags;
    }

    @Data
    @Accessors(chain = true)
    public static class Item implements Serializable {
        private String episodeKey;
        private List<String> language;
        private String magnet;
        private Long publishedAt;
        private String releaseId;
        private String resolution;
        private String subtitle;
        private String title;
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
