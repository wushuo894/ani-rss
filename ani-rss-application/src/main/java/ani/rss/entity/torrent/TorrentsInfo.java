package ani.rss.entity.torrent;

import ani.rss.commons.FileUtils;
import ani.rss.enums.TorrentsStateEnum;
import cn.hutool.core.util.NumberUtil;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 种子信息
 */
@Data
@Accessors(chain = true)
@Schema(description = "种子信息")
public class TorrentsInfo implements Serializable {
    @Schema(description = "id")
    private String id;

    /**
     * hash
     */
    @Schema(description = "hash")
    private String hash;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private TorrentsStateEnum state;

    /**
     * 分类
     */
    @Schema(description = "分类")
    private String category;

    /**
     * 标签
     */
    @Schema(description = "标签")
    private List<String> tagList;

    /**
     * 已下载的大小
     */
    @Schema(description = "已完成的传输数据量（bytes）")
    private Long completed;

    /**
     * 大小
     */
    @Schema(description = "所选文件的总大小（bytes）")
    private Long size;

    /**
     * 进度
     */
    @Schema(description = "进度")
    private Double progress;

    /**
     * 大小
     */
    @Schema(description = "大小(字符串)")
    private String formatSize;

    /**
     * 下载位置
     */
    @Schema(description = "下载位置")
    @SerializedName(value = "savePath", alternate = "save_path")
    private String savePath;

    /**
     * 文件列表
     */
    @Schema(description = "文件列表")
    private Supplier<List<String>> filesSupplier;

    public TorrentsInfo progress(long completed, long size) {
        if (size < 1) {
            size = 1;
            this.setProgress(0.0);
        } else {
            this.setProgress(
                    NumberUtil.round((completed * 1.0 / size) * 100, 2).doubleValue()
            );
        }

        String formatSize = FileUtils.formatSize(size, true);

        this.setCompleted(completed);
        this.setSize(size);
        this.setFormatSize(formatSize);
        return this;
    }

    public Boolean finished() {
        if (Objects.nonNull(progress)) {
            if (progress < 100) {
                return false;
            }
        }

        // 下载完成的状态
        return List.of(
                TorrentsStateEnum.queuedUP,
                TorrentsStateEnum.uploading,
                TorrentsStateEnum.stalledUP,
                TorrentsStateEnum.stoppedUP
        ).contains(state);
    }

}


