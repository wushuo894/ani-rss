package ani.rss.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum NotificationTypeEnum {
    EMBY_REFRESH,
    MAIL,
    SERVER_CHAN,
    SYSTEM,
    TELEGRAM,
    WEB_HOOK,
    SHELL,
    FILE_MOVE,
    OPEN_LIST_UPLOAD
}
