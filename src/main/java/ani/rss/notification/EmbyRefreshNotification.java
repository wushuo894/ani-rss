package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.AfdianUtil;
import ani.rss.util.EmbyUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class EmbyRefreshNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Assert.isTrue(AfdianUtil.verifyExpirationTime(), "未解锁捐赠, 无法使用Emby媒体库刷新");

        Long embyDelayed = notificationConfig.getEmbyDelayed();
        if (embyDelayed > 0) {
            ThreadUtil.sleep(embyDelayed, TimeUnit.SECONDS);
        }
        try {
            EmbyUtil.refresh(notificationConfig);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
