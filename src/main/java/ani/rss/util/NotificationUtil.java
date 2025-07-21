package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.notification.BaseNotification;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ExecutorBuilder;
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
        Boolean isMessage = Opt.ofNullable(ani)
                .map(Ani::getMessage)
                .orElse(true);

        if (!isMessage) {
            // 未开启此订阅通知
            return;
        }

        List<NotificationConfig> notificationConfigList = config.getNotificationConfigList();

        for (NotificationConfig notificationConfig : notificationConfigList) {
            Boolean enable = notificationConfig.getEnable();
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

            BaseNotification baseNotification = ReflectUtil.newInstance(notificationType.getAClass());
            EXECUTOR_SERVICE.execute(() -> {
                try {
                    baseNotification.send(notificationConfig, ani, text, notificationStatusEnum);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }
}
