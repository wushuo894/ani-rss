package ani.rss.enums;

import lombok.Getter;

@Getter
public enum MessageEnum {
    /**
     * 开始下载
     */
    DOWNLOAD_START("🎈"),
    /**
     * 下载完成
     */
    DOWNLOAD_END("🎉"),
    /**
     * 缺集
     */
    OMIT("⚠"),
    /**
     * 错误
     */
    ERROR("❌");

    private final String emoji;

    MessageEnum(String emoji) {
        this.emoji = emoji;
    }
}
