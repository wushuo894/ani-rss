package ani.rss.entity.torrent;

import ani.rss.commons.FileUtils;
import ani.rss.enums.TorrentsStateEnum;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class TransmissionTorrentsInfo implements Serializable {
    private Result result;

    @Data
    @Accessors(chain = true)
    public static class Result implements Serializable {
        private List<Torrent> torrents;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Torrent extends TorrentsInfo implements Serializable {
        /**
         * HASH
         */
        @SerializedName(value = "hashString", alternate = "hash_string")
        private String hashString;

        /**
         * 标签
         */
        private List<String> labels;

        /**
         * 已下载大小
         */
        @SerializedName(value = "haveValid", alternate = "have_valid")
        private Long haveValid;

        /**
         * 大小
         */
        @SerializedName(value = "totalSize", alternate = "total_size")
        private Long totalSize;

        /**
         * 下载位置
         */
        @SerializedName(value = "downloadDir", alternate = "download_dir")
        private String downloadDir;

        /**
         * 状态
         */
        private Integer status;

        /**
         * 已完成
         */
        @SerializedName(value = "isFinished", alternate = "is_finished")
        private Boolean isFinished;

        /**
         * 文件列表
         */
        private List<FileEntity> files;

        public TorrentsInfo toTorrentsInfo() {
            TorrentsStateEnum torrentsState = getTorrentsStateEnum();

            return progress(haveValid, totalSize)
                    .setHash(hashString)
                    .setTagList(labels)
                    .setCompleted(haveValid)
                    .setSize(totalSize)
                    .setSavePath(FileUtils.getAbsolutePath(downloadDir))
                    .setState(torrentsState)
                    .setFilesSupplier(() ->
                            getFiles()
                                    .stream()
                                    .map(FileEntity::getName)
                                    .toList()
                    );
        }

        private TorrentsStateEnum getTorrentsStateEnum() {
            // 做种中
            if (status == 6) {
                return TorrentsStateEnum.stalledUP;
            }

            // 已完成
            if (isFinished) {
                return TorrentsStateEnum.stoppedUP;
            }

            return TorrentsStateEnum.downloading;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class FileEntity implements Serializable {
        private String name;
    }
}
