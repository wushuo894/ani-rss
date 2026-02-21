package ani.rss.action;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.NotificationTypeEnum;
import ani.rss.notification.BaseNotification;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.BgmUtil;
import ani.rss.util.other.NotificationUtil;
import ani.rss.util.other.TmdbUtils;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.IOException;
import java.util.Optional;

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
            resultSuccess();
        } catch (Exception e) {
            resultErrorMsg(e.getMessage());
        }
    }

}
