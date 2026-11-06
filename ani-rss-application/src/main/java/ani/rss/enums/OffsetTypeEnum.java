package ani.rss.enums;

import ani.rss.offset.AutoOffset;
import ani.rss.offset.BaseOffset;
import ani.rss.offset.BgmOffset;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum OffsetTypeEnum {
    AUTO_OFFSET(AutoOffset.class),
    BGM_OFFSET(BgmOffset.class);

    @Getter
    private final Class<? extends BaseOffset> baseOffsetClass;
}
