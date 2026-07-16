package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.service.EmbyRefreshService;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EmbyRefreshNotification implements BaseNotification {

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
        Long embyDelayed = notificationConfig.getEmbyDelayed();
        if (embyDelayed > 0) {
            ThreadUtil.sleep(embyDelayed, TimeUnit.SECONDS);
        }
        try {
            EmbyRefreshService embyRefreshService = SpringUtil.getBean(EmbyRefreshService.class);
            embyRefreshService.refresh(notificationConfig);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
