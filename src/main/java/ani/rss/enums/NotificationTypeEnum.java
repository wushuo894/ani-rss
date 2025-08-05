package ani.rss.enums;

import ani.rss.notification.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NotificationTypeEnum {
    EMBY_REFRESH(EmbyRefreshNotification.class),
    MAIL(MailNotification.class),
    SERVER_CHAN(ServerChanNotification.class),
    SYSTEM(SystemNotification.class),
    TELEGRAM(TelegramNotification.class),
    WEB_HOOK(WebHookNotification.class),
    SHELL(ShellNotification.class);

    @Getter
    private final Class<? extends BaseNotification> aClass;
}
