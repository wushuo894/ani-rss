package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * EmbyWebHook
 */
@Data
@Accessors(chain = true)
public class EmbyWebHook implements Serializable {

    @SerializedName(value = "title", alternate = "Title")
    private String title;

    @SerializedName(value = "description", alternate = "Description")
    private String description;

    @SerializedName(value = "date", alternate = "Date")
    private String date;

    @SerializedName(value = "event", alternate = "Event")
    private String event;

    @SerializedName(value = "severity", alternate = "Severity")
    private String severity;

    @SerializedName(value = "user", alternate = "User")
    private User user;

    @SerializedName(value = "server", alternate = "Server")
    private Server server;

    @SerializedName(value = "item", alternate = "Item")
    private Item item;

    @SerializedName(value = "playbackInfo", alternate = "PlaybackInfo")
    private PlaybackInfo playbackInfo;

    /**
     * 项目信息
     */
    @Data
    @Accessors(chain = true)
    public static class Item implements Serializable {
        /**
         * 文件路径
         */
        @SerializedName(value = "path", alternate = "Path")
        private String path;

        /**
         * 剧集名
         */
        @SerializedName(value = "seriesName", alternate = "SeriesName")
        private String seriesName;

        /**
         * 文件名
         */
        @SerializedName(value = "fileName", alternate = "FileName")
        private String fileName;
    }

    /**
     * 用户信息
     */
    @Data
    @Accessors(chain = true)
    public static class User implements Serializable {
        /**
         * 用户 Id
         */
        @SerializedName(value = "id", alternate = "Id")
        private String id;

        /**
         * 用户名称
         */
        @SerializedName(value = "name", alternate = "Name")
        private String name;
    }

    /**
     * 服务器信息
     */
    @Data
    @Accessors(chain = true)
    public static class Server implements Serializable {
        /**
         * 服务器 Id
         */
        @SerializedName(value = "id", alternate = "Id")
        private String id;

        /**
         * 服务器名称
         */
        @SerializedName(value = "name", alternate = "Name")
        private String name;

        /**
         * 服务器版本号
         */
        @SerializedName(value = "version", alternate = "Version")
        private String version;
    }

    /**
     * 播放信息
     */
    @Data
    @Accessors(chain = true)
    public static class PlaybackInfo implements Serializable {
        /**
         * 是否播放完成
         */
        @SerializedName(value = "playedToCompletion", alternate = "PlayedToCompletion")
        private Boolean playedToCompletion;
    }

}
