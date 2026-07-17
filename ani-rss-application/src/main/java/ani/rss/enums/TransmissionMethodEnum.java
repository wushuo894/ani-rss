package ani.rss.enums;

import lombok.Getter;

public enum TransmissionMethodEnum {
    torrentAdd("torrent_add"),
    torrentGet("torrent_get"),
    torrentRemove("torrent_remove"),
    torrentRenamePath("torrent_rename_path"),
    torrentSet("torrent_set"),
    torrentSetLocation("torrent_set_location");

    TransmissionMethodEnum(String value) {
        this.value = value;
    }

    @Getter
    private final String value;
}
