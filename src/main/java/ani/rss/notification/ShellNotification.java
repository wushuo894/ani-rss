package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Shell
 */
@Slf4j
public class ShellNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String shell = notificationConfig.getShell();
        int aliveLimit = notificationConfig.getAliveLimit();
        Assert.notBlank(shell, "shell 不能为空");
        notificationConfig.setNotificationTemplate(shell);
        shell = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        shell = shell.trim();
        Process process = RuntimeUtil.exec(shell);

        long pid = process.pid();
        log.info("pid: {}", pid);

        process.onExit()
                .thenAccept(result -> {
                    int exitValue = result.exitValue();
                    log.info("已退出 pid: {}, exit: {}", pid, exitValue);
                });

        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(aliveLimit, TimeUnit.SECONDS);
            if (!process.isAlive()) {
                return;
            }
            log.info("存活超时已强制停止 pid: {}", pid);
            process.destroyForcibly();
        });

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return process.exitValue() == 0;
    }
}
