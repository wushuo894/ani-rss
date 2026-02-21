package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.other.MenuUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Objects;

/**
 * SystemMsg
 */
@Slf4j
public class SystemNotification implements BaseNotification {

    /**
     * 测试
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     */
    @Override
    public void test(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        send(notificationConfig, ani, text, notificationStatusEnum);
    }

    /**
     * 发送通知
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     * @return 是否成功
     */
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
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

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        trayIcon.displayMessage("ani-rss", notificationTemplate, type);
        return true;
    }
}
