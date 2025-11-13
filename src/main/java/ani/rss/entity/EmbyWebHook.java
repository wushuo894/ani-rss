package ani.rss.entity;

import ani.rss.entity.tmdb.Tmdb;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.RenameUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

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

    /**
     * 是否匹配到订阅
     *
     * @param ani
     * @return
     */
    public Boolean equalsAni(Ani ani) {
        String fileName = item.getFileName();
        if (!ReUtil.contains(StringEnum.SEASON_REG, fileName)) {
            return false;
        }

        // 季
        int season = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, fileName, 1));

        if (season != ani.getSeason()) {
            return false;
        }

        String bgmUrl = ani.getBgmUrl();
        if (StrUtil.isBlank(bgmUrl)) {
            // bgmUrl为空
            return false;
        }

        String path = item.getPath();
        String parent = new File(path).getParent();
        File downloadPath = DownloadService.getDownloadPath(ani);
        if (downloadPath.toString().equals(parent)) {
            // 路径相同
            return true;
        }

        String title = ani.getTitle();
        title = RenameUtil.renameDel(title, false);
        String seriesName = item.getSeriesName();
        if (title.equals(seriesName)) {
            // 名称与季相同
            return true;
        }

        Tmdb tmdb = ani.getTmdb();
        if (Objects.isNull(tmdb)) {
            return false;
        }

        // 对比tmdb名称
        String name = tmdb.getName();
        if (StrUtil.isNotBlank(name)) {
            if (name.equals(seriesName)) {
                return true;
            }
        }

        // 对比tmdb原名
        String originalName = tmdb.getOriginalName();
        if (StrUtil.isNotBlank(originalName)) {
            return originalName.equals(seriesName);
        }
        return false;
    }

}
