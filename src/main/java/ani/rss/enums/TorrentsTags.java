package ani.rss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TorrentsTags {
    ANI_RSS("ani-rss"),
    RENAME("RENAME"),
    BACK_RSS("备用RSS"),
    DOWNLOAD_COMPLETE("下载完成"),
    OPEN_LIST("OpenList"),
    UPLOAD_COMPLETED("上传完成");

    private final String value;
}
