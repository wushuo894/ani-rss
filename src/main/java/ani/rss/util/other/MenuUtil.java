package ani.rss.util.other;

import ani.rss.Main;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MenuUtil {

    public static TrayIcon trayIcon;

    public static void start() {
        // 仅在添加--gui参数时启动托盘
        if (Main.ARGS.contains("--gui")) {
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

        // 获取屏幕缩放
        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        AffineTransform tx = gc.getDefaultTransform();
        double uiScaleX = tx.getScaleX();
        double uiScaleY = tx.getScaleY();


        SystemTray tray = SystemTray.getSystemTray();
        trayIcon = new TrayIcon(
                Toolkit.getDefaultToolkit().getImage(
                        ResourceUtil.getResource("image/icon-64.png")
                ), "ani-rss");
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);
        // 使用JDialog 作为JPopupMenu载体
        JDialog jDialog = new JDialog();
        // 关闭JDialog的装饰器
        jDialog.setUndecorated(true);
        // jDialog作为JPopupMenu载体不需要多大的size
        jDialog.setSize(1, 1);

        // 创建JPopupMenu
        // 重写firePopupMenuWillBecomeInvisible
        // 消失后将绑定的组件一起消失
        JPopupMenu jPopupMenu = new JPopupMenu() {
            @Override
            public void firePopupMenuWillBecomeInvisible() {
                // JPopupMenu不可见时绑定载体组件jDialog也不可见
                jDialog.setVisible(false);
            }
        };
        jPopupMenu.setSize(100, 30);

        // 添加菜单选项
        JMenuItem webui = jPopupMenu.add(new JMenuItem("打开webui"));
        webui.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URL("http://127.0.0.1:" + ServerUtil.HTTP_PORT).toURI());
                } catch (Exception ex) {
                    log.error("打开webui失败", ex);
                }
            }
        });
        JMenuItem exit = jPopupMenu.add(new JMenuItem("退出"));
        exit.addActionListener(e -> {
            log.info("使用系统托盘退出");
            System.exit(0);
        });

        // 防止连点
        AtomicBoolean clicked = new AtomicBoolean(true);

        // 给托盘图标添加鼠标监听
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseReleased(MouseEvent e) {
                // 左键点击
                if (e.getButton() == 1 && clicked.get()) {
                    clicked.set(false);
                    // 直接打开webui
                    try {
                        Desktop.getDesktop().browse(new URL("http://127.0.0.1:" + ServerUtil.HTTP_PORT).toURI());
                    } catch (Exception ex) {
                        log.error("打开webui失败", ex);
                    }
                    ThreadUtil.execute(() -> {
                        ThreadUtil.sleep(1000);
                        clicked.set(true);
                    });
                }

                if (e.getButton() == 3 && e.isPopupTrigger()) {
                    // 右键点击弹出JPopupMenu绑定的载体以及JPopupMenu
                    // 适配非1倍缩放的屏幕
                    jDialog.setLocation((int) (e.getX() / uiScaleX + 5),
                            (int) (e.getY() / uiScaleY - 5 - jPopupMenu.getHeight()));
                    // 显示载体
                    jDialog.setVisible(true);
                    // 在载体的0,0处显示对话框
                    jPopupMenu.show(jDialog, 0, 0);
                }
            }
        });
    }
}
