package ani.rss.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NotificationTypeEnum {
    EMBY_REFRESH,
    MAIL,
    SERVER_CHAN,
    // 保留旧配置的反序列化兼容性，托盘通知已移除。
    SYSTEM,
    TELEGRAM,
    WEB_HOOK,
    SHELL,
    FILE_MOVE,
    OPEN_LIST_UPLOAD,
    BARK,
    ASTRBOT
}
