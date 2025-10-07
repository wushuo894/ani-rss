package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 视频列表
 */
@Data
@Accessors(chain = true)
public class PlayItem implements Serializable {

    private String title;

    private String filename;

    private String name;

    private Long lastModify;

    private Integer episodeNumber;

    private List<Subtitles> subtitles;

    @Data
    @Accessors(chain = true)
    public static class Subtitles {
        private String html;
        private String name;
        private String url;
        private String type;
    }

}
