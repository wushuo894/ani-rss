package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.util.MenuUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.Objects;

@Slf4j
public class SystemMsg implements Message {
    @Override
    public Boolean send(Config config, Ani ani, MessageEnum messageEnum, String text) {
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
        if (Objects.nonNull(messageEnum)) {
            if (messageEnum.name().equals(MessageEnum.ERROR.name())) {
                type = TrayIcon.MessageType.ERROR;
            }
        }
        trayIcon.displayMessage("ani-rss", text, type);
        return true;
    }
}
