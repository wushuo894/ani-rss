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

    /**
     * 显示标题
     */
    private String title;

    /**
     * 路径+文件名 bash64
     */
    private String filename;

    /**
     * 文件名
     */
    private String name;

    /**
     * 最后修改日期
     */
    private Long lastModify;

    /**
     * 集数
     */
    private Double episode;

    /**
     * 文件大小 MB
     */
    private String size;

    /**
     * 扩展名
     */
    private String extName;

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
