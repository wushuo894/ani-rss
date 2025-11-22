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

    @SerializedName(value = "item", alternate = "Item")
    private Item item;

    @SerializedName(value = "event", alternate = "Event")
    private String event;

    @SerializedName(value = "playbackInfo", alternate = "PlaybackInfo")
    private PlaybackInfo playbackInfo;

    @Data
    @Accessors(chain = true)
    public static class Item implements Serializable {
        @SerializedName(value = "path", alternate = "Path")
        private String path;
        @SerializedName(value = "seriesName", alternate = "SeriesName")
        private String seriesName;
        @SerializedName(value = "fileName", alternate = "FileName")
        private String fileName;
    }

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
