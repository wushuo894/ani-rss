package ani.rss.util.other;

import ani.rss.entity.Global;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.URL;

@Slf4j
public class SystemTrayUtil {

    public static TrayIcon TRAY_ICON;

    public static void start() {
        // 仅在添加--gui参数时启动托盘
        if (Global.ARGS.contains("--gui")) {
            try {
                showSystemTray();
                log.info("启动系统托盘已启动");
            } catch (Exception e) {
                log.error("启动系统托盘失败", e);
                System.exit(1);
            }
        }
    }

    /**
     * 显示系统托盘
     * 参考 <a href="https://lanlan2017.github.io/blog/2b294c47/">链接</a>
     */
    private static void showSystemTray() throws AWTException {
        if (!SystemTray.isSupported()) {
            log.error("系统托盘不支持, 将以命令行方式启动");
            // 直接输出到控制台，不使用logger
            System.out.print("系统托盘不支持, 将以命令行方式启动");
            return;
        }

        // 创建JPopupMenu
        // 重写firePopupMenuWillBecomeInvisible
        // 消失后将绑定的组件一起消失
        PopupMenu popupMenu = new PopupMenu();

        // 添加菜单选项
        MenuItem webui = popupMenu.add(new MenuItem("WebUI", new MenuShortcut(KeyEvent.VK_W)));
        webui.addActionListener(e -> {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            try {
                String port = SpringUtil.getProperty("server.port");
                Desktop.getDesktop().browse(new URL("http://127.0.0.1:" + port).toURI());
            } catch (Exception ex) {
                log.error("打开webui失败", ex);
            }
        });

        MenuItem config = popupMenu.add(new MenuItem("Config", new MenuShortcut(KeyEvent.VK_C)));
        config.addActionListener(e -> {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            Desktop.getDesktop().browseFileDirectory(ConfigUtil.getConfigFile());
        });

        MenuItem exit = popupMenu.add(new MenuItem("Quit", new MenuShortcut(KeyEvent.VK_Q)));
        exit.addActionListener(e -> {
            log.info("使用系统托盘退出");
            System.exit(1);
        });

        SystemTray tray = SystemTray.getSystemTray();
        TRAY_ICON = new TrayIcon(
                Toolkit.getDefaultToolkit().getImage(
                        ResourceUtil.getResource("image/icon-64.png")
                ), "ani-rss", popupMenu);
        TRAY_ICON.setImageAutoSize(true);

        tray.add(TRAY_ICON);
    }
}
