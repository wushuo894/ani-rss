package ani.rss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TmdbTypeEnum {
    MOVIE("movie"), TV("tv");

    @Getter
    private final String value;
}
