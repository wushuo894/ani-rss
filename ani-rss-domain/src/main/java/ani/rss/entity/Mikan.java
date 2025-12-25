package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * mikan
 */
@Data
@Accessors(chain = true)
public class Mikan implements Serializable {

    private List<Season> seasons;

    private List<Item> items;

    /**
     * 季度信息
     */
    @Data
    @Accessors(chain = true)
    public static class Season implements Serializable {
        /**
         * 年
         */
        private Integer year;
        /**
         * 季度
         */
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
        private String label;
        /**
         * 番剧
         */
        private List<Ani> items;
    }

    /**
     * 字幕组
     */
    @Data
    @Accessors(chain = true)
    public static class Group implements Serializable {
        /**
         * 匹配
         */
        private List<List<String>> matchList;

        /**
         * 字幕组名称
         */
        private String label;
        /**
         * 标签
         */
        private Set<String> tags;
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
    }
}
