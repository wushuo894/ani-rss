package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.MenuUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Objects;

/**
 * SystemMsg
 */
@Slf4j
public class SystemNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Config config = ConfigUtil.CONFIG;
        String template = config.getNotificationTemplate();
        template = replaceNotificationTemplate(ani, template, text, notificationStatusEnum);

        String notificationTemplate = notificationConfig.getNotificationTemplate();
        notificationTemplate = notificationTemplate.replace("${notification}", template);
        notificationTemplate = replaceNotificationTemplate(ani, notificationTemplate, text, notificationStatusEnum);
        if (!SystemTray.isSupported()) {
            log.error("SystemTray is not supported");
            return false;
        }
        TrayIcon trayIcon = MenuUtil.trayIcon;
        if (Objects.isNull(trayIcon)) {
            log.error("未开启系统托盘 添加--gui参数启动");
            return false;
        }
        TrayIcon.MessageType type = TrayIcon.MessageType.INFO;
        if (Objects.nonNull(notificationStatusEnum)) {
            if (notificationStatusEnum.name().equals(NotificationStatusEnum.ERROR.name())) {
                type = TrayIcon.MessageType.ERROR;
            }
        }
        trayIcon.displayMessage("ani-rss", notificationTemplate, type);
        return true;
    }
}
