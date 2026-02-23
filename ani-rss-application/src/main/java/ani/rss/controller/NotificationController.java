package ani.rss.controller;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.Result;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.notification.BaseNotification;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import ani.rss.util.other.NotificationUtil;
import ani.rss.util.other.TmdbUtils;
import cn.hutool.core.util.ReflectUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wushuo.tmdb.api.entity.Tmdb;

import java.util.Optional;

@RestController
public class NotificationController {

    @Operation(summary = "测试通知")
    @PostMapping("/testNotification")
    public Result<Void> testNotification(@RequestBody NotificationConfig notificationConfig) {
        NotificationTypeEnum notificationType = notificationConfig.getNotificationType();
        Class<? extends BaseNotification> aClass = NotificationUtil.NOTIFICATION_MAP.get(notificationType);
        BaseNotification baseNotification = ReflectUtil.newInstance(aClass);
        Ani ani = AniUtil.createAni();
        BgmInfo bgmInfo = BgmUtil.getBgmInfo("292970", true);
        BgmUtil.toAni(bgmInfo, ani);

        String image = ani.getImage();
        String title = ani.getTitle();

        ani.setCover(AniUtil.saveJpg(image))
                .setCurrentEpisodeNumber(6)
                .setTotalEpisodeNumber(12)
                .setSubgroup("未知字幕组");

        Optional<Tmdb> tmdb = TmdbUtils.getTmdbTv(title);

        tmdb.ifPresent(ani::setTmdb);

        try {
            baseNotification.test(notificationConfig, ani, "test", NotificationStatusEnum.DOWNLOAD_START);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "新的通知")
    @PostMapping("/newNotification")
    public Result<NotificationConfig> newNotification() {
        NotificationConfig notificationConfig = NotificationConfig.createNotificationConfig();
        return Result.success(notificationConfig);
    }

}
