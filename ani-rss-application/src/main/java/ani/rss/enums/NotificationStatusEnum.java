package ani.rss.enums;

import lombok.Getter;

@Getter
public enum NotificationStatusEnum {
    /**
     * 开始下载
     */
    DOWNLOAD_START("🎈", "开始下载"),
    /**
     * 下载完成
     */
    DOWNLOAD_END("🎉", "下载完成"),
    /**
     * 缺集
     */
    OMIT("⛔", "缺少集数"),
    /**
     * 错误
     */
    ERROR("❌", "发生错误"),
    /**
     * 订阅完结
     */
    COMPLETED("🎊", "订阅完结"),
    /**
     * 摸鱼检测
     */
    PROCRASTINATING("🐟", "摸鱼检测");

    private final String emoji;
    private final String action;

    NotificationStatusEnum(String emoji, String action) {
        this.emoji = emoji;
        this.action = action;
    }
}
