package ani.rss.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

@Slf4j
public class MenuUtil {
    public static void start(String[] args) {
        args = ObjectUtil.defaultIfNull(args, new String[]{});
        // 仅在添加--gui参数时启动托盘
        if (Arrays.asList(args).contains("--gui")) {
            try {
                // 检查是否有其他实例在运行
                if (!Arrays.asList(args).contains("--multi")) {
                    checkSingleRun();
                }
                showSystemTray();
                // 直接输出到控制台，不使用 logger
                System.out.println("启动系统托盘已启动");
            } catch (Exception e) {
                log.error("启动系统托盘失败", e);
                System.exit(1);
            }
        }
    }


    /**
     * 检查是否有其他实例在运行
     * 如果有，抛出异常
     */
    private static void checkSingleRun() throws Exception {
        File file = new File(System.getProperty("user.home"), "ani-rss.lock");
        if (file.exists()) {
            // 有其他实例在运行
            // 删除锁文件，排除异常退出的情况
            FileUtil.del(file);
            throw new Exception("另一个ani-rss实例正在运行");
        }
        Files.createFile(file.toPath());

        // 每隔1秒检查文件是否存在，如果不存在则创建
        // NOTE: 没有文件锁的丑陋做法
        ThreadUtil.schedule(ThreadUtil.createScheduledExecutor(1), () -> {
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 1000, true);

        // 退出时删除锁文件
        Runtime.getRuntime().addShutdownHook(new Thread(file::delete));
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
        TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(ResourceUtil.getResource("icon" + ".png"
        )), "ani-rss");
        trayIcon.setImageAutoSize(true);
        trayIcon.displayMessage("ani-rss", "启动成功", TrayIcon.MessageType.INFO);
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

        //添加菜单选项
        JMenuItem webui = jPopupMenu.add(new JMenuItem("打开webui"));
        webui.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URL("http://127.0.0.1:7789").toURI());
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

        // 给托盘图标添加鼠标监听
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //左键点击
                if (e.getButton() == 1) {
                    // 直接打开webui
                    try {
                        Desktop.getDesktop().browse(new URL("http://127.0.0.1:" + ServerUtil.PORT).toURI());
                    } catch (Exception ex) {
                        log.error("打开webui失败", ex);
                    }
                } else if (e.getButton() == 3 && e.isPopupTrigger()) {
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
