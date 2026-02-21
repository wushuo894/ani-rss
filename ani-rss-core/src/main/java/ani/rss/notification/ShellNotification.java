package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Assert;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Shell
 */
@Slf4j
public class ShellNotification implements BaseNotification {

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
        String shell = notificationConfig.getShell();
        int aliveLimit = notificationConfig.getAliveLimit();
        Assert.notBlank(shell, "shell 不能为空");
        notificationConfig.setNotificationTemplate(shell);
        shell = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        shell = shell.trim();

        log.debug(shell);

        Process process;
        try {
            process = new ProcessBuilder(getShellCommand(shell))
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }

        long pid = process.pid();
        log.info("pid: {}", pid);

        CompletableFuture<String> outputFuture = readStreamAsync(process.getInputStream());

        process.onExit()
                .thenAccept(result -> {
                    try {
                        String output = outputFuture.get(5, TimeUnit.SECONDS);
                        log.debug(output);
                    } catch (Exception ignored) {
                    }

                    int exitValue = result.exitValue();
                    log.info("已退出 pid: {}, exit: {}", pid, exitValue);
                });
        try {
            boolean b = process.waitFor(aliveLimit, TimeUnit.SECONDS);
            if (!b) {
                log.info("存活超时已强制停止 pid: {}", pid);
                process.destroyForcibly();
                return false;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return process.exitValue() == 0;
    }

    private static String[] getShellCommand(String fullCommand) {
        boolean isWindows = SystemUtil.getOsInfo().isWindows();
        return isWindows ?
                new String[]{"cmd.exe", "/c", fullCommand} :
                new String[]{"sh", "-c", fullCommand};
    }

    private static CompletableFuture<String> readStreamAsync(InputStream input) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new CompletionException("流读取异常", e);
            }
        });
    }

}
