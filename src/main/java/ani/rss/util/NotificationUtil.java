package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.notification.BaseNotification;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class NotificationUtil {
    private static final ExecutorService EXECUTOR_SERVICE = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    /**
     * 发送通知
     *
     * @param config
     * @param ani
     * @param text
     * @param notificationStatusEnum
     */
    public static synchronized void send(Config config, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Boolean isMessage = ani.getMessage();

        if (!isMessage) {
            // 未开启此订阅通知
            return;
        }

        List<NotificationConfig> notificationConfigList = config.getNotificationConfigList();

        for (NotificationConfig notificationConfig : notificationConfigList) {
            boolean enable = notificationConfig.getEnable();
            int retry = notificationConfig.getRetry();
            NotificationTypeEnum notificationType = notificationConfig.getNotificationType();
            List<NotificationStatusEnum> statusList = notificationConfig.getStatusList();

            if (!enable) {
                // 未开启
                continue;
            }

            if (!statusList.contains(notificationStatusEnum)) {
                // 未启用 通知状态
                continue;
            }

            Class<? extends BaseNotification> aClass = notificationType.getAClass();

            BaseNotification baseNotification = ReflectUtil.newInstance(aClass);
            EXECUTOR_SERVICE.execute(() -> {
                int currentRetry = 0;
                do {
                    if (currentRetry > 0) {
                        log.warn("通知失败 正在重试 第{}次 {}", currentRetry, aClass.getName());
                    }
                    try {
                        baseNotification.send(notificationConfig, ani, text, notificationStatusEnum);
                        return;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    currentRetry += 1;
                    ThreadUtil.sleep(1000);
                } while (currentRetry < retry);
            });
        }
    }
}
