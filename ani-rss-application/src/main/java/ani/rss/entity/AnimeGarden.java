package ani.rss.entity;

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
public class AnimeGarden implements Serializable {

    @Data
    @Accessors(chain = true)
    public static class Week implements Serializable {
        private String weekLabel;
        private List<Subject> subjects;
    }

    @Data
    @Accessors(chain = true)
    public static class Subject implements Serializable {
        private String id;
        private String name;
        private List<String> keywords;
        private Date activedAt;
        private String isArchived;
        private String weekLabel;
        private Boolean exists;
    }

    @Data
    @Accessors(chain = true)
    public static class Group implements Serializable {
        private String id;
        private String name;
        private Date lastUpdatedAt;
        private List<Item> items;
        private String rss;
        private String bgmId;

        /**
         * Regex
         */
        private List<List<RegexItem>> regexList;
        private Set<String> tags;
    }

    @Data
    @Accessors(chain = true)
    public static class Item implements Serializable {
        private String id;
        private String provider;
        private String providerId;
        private String title;
        private String href;
        private String type;
        private String magnet;
        private Long size;
        private Date createdAt;
        private Date fetchedAt;
        private String subjectId;
        private Publisher publisher;
        private Fansub fansub;
    }

    @Data
    @Accessors(chain = true)
    public static class Publisher implements Serializable {
        private String id;
        private String name;
        private String avatar;
    }

    @Data
    @Accessors(chain = true)
    public static class Fansub implements Serializable {
        private String id;
        private String name;
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
