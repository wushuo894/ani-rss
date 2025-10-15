package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.Tmdb;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.notification.BaseNotification;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.Date;

/**
 * 通知
 */
@Auth
@Path("/notification")
public class NotificationAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String type = request.getParam("type");
        if ("test".equals(type)) {
            test();
            return;
        }

        if ("add".equals(type)) {
            add();
        }

    }

    private void add() {
        NotificationConfig notificationConfig = NotificationConfig.createNotificationConfig();
        resultSuccess(notificationConfig);
    }

    private void test() {
        NotificationConfig notificationConfig = getBody(NotificationConfig.class);
        NotificationTypeEnum notificationType = notificationConfig.getNotificationType();
        BaseNotification baseNotification = ReflectUtil.newInstance(notificationType.getAClass());
        Ani ani = Ani.createAni();
        ani.setBgmUrl("https://bgm.tv/subject/424883");
        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani);
        String image = bgmInfo.getImage();
        ani.setCover(AniUtil.saveJpg(image))
                .setImage(image)
                .setTitle("不时用俄语小声说真心话的邻桌艾莉同学")
                .setSeason(1)
                .setCurrentEpisodeNumber(2)
                .setTotalEpisodeNumber(12)
                .setScore(8.0)
                .setThemoviedbName("test")
                .setYear(2024)
                .setSubgroup("未知字幕组")
                .setTmdb(
                        new Tmdb()
                                .setId("235758")
                                .setName("不时用俄语小声说真心话的邻桌艾莉同学")
                                .setDate(new Date())
                );
        Boolean test = baseNotification.send(notificationConfig, ani, "test", NotificationStatusEnum.DOWNLOAD_START);
        if (test) {
            resultSuccess();
            return;
        }
        resultError();
    }

}
