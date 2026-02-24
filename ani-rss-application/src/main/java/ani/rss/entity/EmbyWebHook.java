package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * EmbyWebHook
 */
@Data
@Accessors(chain = true)
@Schema(description = "EmbyWebHook")
public class EmbyWebHook implements Serializable {

    @SerializedName(value = "title", alternate = "Title")
    @Schema(description = "标题")
    private String title;

    @SerializedName(value = "description", alternate = "Description")
    @Schema(description = "描述")
    private String description;

    @SerializedName(value = "date", alternate = "Date")
    @Schema(description = "日期")
    private String date;

    @SerializedName(value = "event", alternate = "Event")
    @Schema(description = "事件")
    private String event;

    @SerializedName(value = "severity", alternate = "Severity")
    @Schema(description = "严重级别")
    private String severity;

    @SerializedName(value = "user", alternate = "User")
    @Schema(description = "用户信息")
    private User user;

    @SerializedName(value = "server", alternate = "Server")
    @Schema(description = "服务器信息")
    private Server server;

    @SerializedName(value = "item", alternate = "Item")
    @Schema(description = "项目信息")
    private Item item;

    @SerializedName(value = "playbackInfo", alternate = "PlaybackInfo")
    @Schema(description = "播放信息")
    private PlaybackInfo playbackInfo;

    /**
     * 项目信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "项目信息")
    public static class Item implements Serializable {
        /**
         * 文件路径
         */
        @SerializedName(value = "path", alternate = "Path")
        @Schema(description = "文件路径")
        private String path;

        /**
         * 剧集名
         */
        @SerializedName(value = "seriesName", alternate = "SeriesName")
        @Schema(description = "剧集名")
        private String seriesName;

        /**
         * 文件名
         */
        @SerializedName(value = "fileName", alternate = "FileName")
        @Schema(description = "文件名")
        private String fileName;
    }

    /**
     * 用户信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "用户信息")
    public static class User implements Serializable {
        /**
         * 用户 Id
         */
        @SerializedName(value = "id", alternate = "Id")
        @Schema(description = "用户 Id")
        private String id;

        /**
         * 用户名称
         */
        @SerializedName(value = "name", alternate = "Name")
        @Schema(description = "用户名称")
        private String name;
    }

    /**
     * 服务器信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "服务器信息")
    public static class Server implements Serializable {
        /**
         * 服务器 Id
         */
        @SerializedName(value = "id", alternate = "Id")
        @Schema(description = "服务器 Id")
        private String id;

        /**
         * 服务器名称
         */
        @SerializedName(value = "name", alternate = "Name")
        @Schema(description = "服务器名称")
        private String name;

        /**
         * 服务器版本号
         */
        @SerializedName(value = "version", alternate = "Version")
        @Schema(description = "服务器版本号")
        private String version;
    }

    /**
     * 播放信息
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "播放信息")
    public static class PlaybackInfo implements Serializable {
        /**
         * 是否播放完成
         */
        @SerializedName(value = "playedToCompletion", alternate = "PlayedToCompletion")
        @Schema(description = "是否播放完成")
        private Boolean playedToCompletion;
    }

}
