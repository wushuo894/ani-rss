package ani.rss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TorrentsTagEnum {
    ANI_RSS("ani-rss"),
    RENAME("RENAME"),
    STANDBY_RSS("备用RSS"),
    DOWNLOAD_COMPLETE("下载完成");

    private final String value;
}
