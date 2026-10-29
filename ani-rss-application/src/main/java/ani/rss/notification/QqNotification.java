package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.web.ContentType;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.QqUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

/**
 * QQ 官方机器人通知
 * <p>
 * 通过 QQ 开放平台接口把 ani-rss 的通知以普通文本消息推送到私聊（C2C）。
 */
@Slf4j
public class QqNotification implements BaseNotification {

    private static final String C2C_BASE = QqUtil.OPEN_API + "/v2/users/";

    /**
     * 测试
     */
    @Override
    public void test(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        if (StrUtil.isBlank(notificationConfig.getQqAppId())) {
            throw new IllegalArgumentException("QQ AppID 为空");
        }
        if (StrUtil.isBlank(notificationConfig.getQqClientSecret())) {
            throw new IllegalArgumentException("QQ AppSecret 为空");
        }
        if (StrUtil.isBlank(notificationConfig.getQqOpenId())) {
            throw new IllegalArgumentException("请先绑定 QQ 用户 OpenID");
        }
        send(notificationConfig, ani, text, notificationStatusEnum);
    }

    /**
     * 发送通知
     */
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String appId = notificationConfig.getQqAppId();
        String clientSecret = notificationConfig.getQqClientSecret();
        String openId = notificationConfig.getQqOpenId();

        if (StrUtil.isBlank(appId) || StrUtil.isBlank(clientSecret)) {
            log.warn("QQ 通知的参数不完整");
            return false;
        }

        if (StrUtil.isBlank(openId)) {
            log.warn("QQ 通知未配置用户 OpenID");
            return false;
        }

        String content = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        String token = QqUtil.getAccessToken(appId, clientSecret);

        JsonObject body = new JsonObject();
        body.addProperty("msg_type", 0);
        body.addProperty("content", content);

        HttpResponse response = HttpReq.post(C2C_BASE + openId + "/messages")
                .contentType(ContentType.JSON)
                .header("Authorization", "QQBot " + token)
                .body(body.toString())
                .execute();
        QqUtil.assertOk(response, "发送 QQ 消息");

        return true;
    }
}
