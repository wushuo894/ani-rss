package ani.rss.entity.torrent;

import ani.rss.commons.FileUtils;
import ani.rss.download.qBittorrent;
import ani.rss.enums.TorrentsStateEnum;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class qBittorrentTorrentsInfo extends TorrentsInfo implements Serializable {

    /**
     * 标签
     */
    @Schema(description = "标签")
    private String tags;

    public TorrentsInfo toTorrentsInfo() {
        // 将标签转换为 List
        List<String> tagList = StrUtil.split(tags, ",", true, true);
        setTagList(tagList);

        // 获取文件列表
        setFilesSupplier(() ->
                qBittorrent.files(this, true)
                        .stream()
                        .filter(fileEntity -> fileEntity.getPriority() > 0)
                        .map(qBittorrentTorrentsInfo.FileEntity::getName)
                        .toList()
        );

        // 处理 Windows 路径
        String savePath = getSavePath();
        setSavePath(FileUtils.getAbsolutePath(savePath));

        // 处理进度与文件大小
        progress(getCompleted(), getSize());

        // 处理下载状态防止空指针
        TorrentsStateEnum state = getState();
        state = ObjectUtil.defaultIfNull(state, TorrentsStateEnum.downloading);

        // 处理 < 5.0.0 的旧状态
        state = switch (state) {
            case pausedDL -> TorrentsStateEnum.stoppedDL;
            case pausedUP -> TorrentsStateEnum.stoppedUP;
            default -> state;
        };

        setState(state);
        return this;
    }

    @Data
    @Accessors(chain = true)
    public static class FileEntity implements Serializable {
        private Integer index;
        private String name;
        private Long size;
        /**
         * 1 允许下载。2 禁止下载
         */
        private Integer priority;
    }
}
