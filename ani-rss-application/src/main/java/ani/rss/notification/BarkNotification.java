package ani.rss.notification;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpResponse;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class BarkNotification implements BaseNotification {
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
        BarkPushBody barkPushBody = toBarkPushBody(notificationConfig, ani, text, notificationStatusEnum);

        String serverUrl = notificationConfig.getBarkServerUrl();

        Assert.notBlank(serverUrl, "请设置 Bark ServerUrl");

        return HttpReq.post(serverUrl + "/push")
                .body(GsonStatic.toJson(barkPushBody))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 转换为BarkPushBody
     *
     * @param notificationConfig     通知设置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     * @return BarkPushBody
     */
    public BarkPushBody toBarkPushBody(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String barkGroup = notificationConfig.getBarkGroup();
        List<String> barkDeviceKeys = notificationConfig.getBarkDeviceKeys();
        String barkLevel = notificationConfig.getBarkLevel();
        Integer barkVolume = notificationConfig.getBarkVolume();
        Boolean barkUseMarkdown = notificationConfig.getBarkUseMarkdown();
        String image = ani.getImage();

        Assert.notEmpty(barkDeviceKeys, "请设置 Bark DeviceKeys");

        String body = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);

        BarkPushBody barkPushBody = new BarkPushBody();

        if (barkUseMarkdown) {
            barkPushBody.setMarkdown(body);
        } else {
            barkPushBody.setBody(body);
        }

        return barkPushBody
                .setTitle("ani-rss")
                .setSubtitle(text)
                .setIcon("https://docs.wushuo.top/favicon.ico")
                .setImage(image)
                .setDeviceKeys(barkDeviceKeys)
                .setGroup(barkGroup)
                .setLevel(barkLevel)
                .setVolume(barkVolume);
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "Bark 推送消息体")
    public static class BarkPushBody {
        @SerializedName("device_keys")
        @Schema(description = "设备键列表")
        private List<String> deviceKeys;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "副标题")
        private String subtitle;
        @Schema(description = "消息内容")
        private String body;
        @Schema(description = "markdown内容")
        private String markdown;
        @Schema(description = "图标")
        private String icon;
        @Schema(description = "图片")
        private String image;
        @Schema(description = "分组")
        private String group;
        @Schema(description = "中断级别")
        private String level;
        @Schema(description = "音量")
        private Integer volume;
    }
}
