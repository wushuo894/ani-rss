package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Mikan {

    private List<Season> seasons;

    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Season {
        private Integer year;
        private String season;
        private Boolean select;
    }

    @Data
    @Accessors(chain = true)
    public static class Item {
        private String label;
        private List<Ani> items;
    }

    @Data
    @Accessors(chain = true)
    public static class Group {
        private String label;
        private String rss;
        private List<TorrentsInfo> items;
    }
}
