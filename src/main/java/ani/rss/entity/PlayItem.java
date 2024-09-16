package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class PlayItem implements Serializable {

    private String title;

    private String filename;

    private List<Subtitles> subtitles;

    @Data
    @Accessors(chain = true)
    public static class Subtitles{
        private String html;
        private String name;
        private String url;
        private String type;
    }

}
