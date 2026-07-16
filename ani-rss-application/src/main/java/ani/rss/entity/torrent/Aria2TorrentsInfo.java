package ani.rss.entity.torrent;

import ani.rss.commons.FileUtils;
import ani.rss.enums.TorrentsStateEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class Aria2TorrentsInfo implements Serializable {
    private List<Torrent> result;

    @Data
    @Accessors(chain = true)
    public static class Bittorrent implements Serializable {
        private Info info;

        @Data
        @Accessors(chain = true)
        public static class Info implements Serializable {
            private String name;
        }
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Torrent extends TorrentsInfo implements Serializable {
        private Bittorrent bittorrent;

        /**
         * HASH
         */
        private String infoHash;

        /**
         * 标签
         */
        private String gid;

        /**
         * 已下载大小
         */
        private Long completedLength;

        /**
         * 大小
         */
        private Long totalLength;

        /**
         * 下载位置
         */
        private String dir;

        /**
         * 状态
         */
        private String status;

        /**
         * 文件列表
         */
        private List<FileEntity> files;

        public TorrentsInfo toTorrentsInfo() {
            Bittorrent.Info info = bittorrent.getInfo();
            String name = info.getName();

            TorrentsStateEnum torrentsStateEnum = getTorrentsStateEnum();

            return progress(completedLength, totalLength)
                    .setTagList(List.of())
                    .setId(gid)
                    .setName(name)
                    .setHash(infoHash)
                    .setState(torrentsStateEnum)
                    .setSavePath(FileUtils.getAbsolutePath(dir))
                    .setFilesSupplier(() ->
                            files.stream()
                                    .map(FileEntity::getPath)
                                    .toList()
                    );
        }

        private TorrentsStateEnum getTorrentsStateEnum() {
            return "complete".equals(status) ?
                    TorrentsStateEnum.stoppedUP : TorrentsStateEnum.downloading;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class FileEntity implements Serializable {
        private String path;
    }
}
