package ani.rss.notification;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * 通过 AstrBot OpenAPI 向指定会话主动推送通知。
 */
public class AstrBotNotification implements BaseNotification {
    /**
     * 使用正式发送流程测试配置，发送失败时向页面返回错误。
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
     * 渲染通知模板，并使用具有 im 权限的 API Key 发送文本消息。
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     * @return 发送成功，失败时抛出异常以触发现有重试机制
     */
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        String host = StrUtil.trim(notificationConfig.getAstrBotHost());
        String apiKey = StrUtil.trim(notificationConfig.getAstrBotApiKey());
        String umo = StrUtil.trim(notificationConfig.getAstrBotUmo());

        Assert.notBlank(host, "请设置 AstrBot 服务地址");
        Assert.notBlank(apiKey, "请设置 AstrBot API Key（需要 im 权限）");
        Assert.notBlank(umo, "请设置 AstrBot 目标 UMO（在目标会话发送 /sid 获取）");

        String message = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);
        Assert.notBlank(message, "AstrBot 通知内容不能为空，请检查通知模版");

        // 统一移除地址末尾的斜杠；通过对象序列化保留模板中的换行、引号等字符。
        String url = host + "/api/v1/im/message";
        return HttpReq.post(url, Map.of("umo", umo, "message", message))
                .header("Authorization", "Bearer " + apiKey)
                .thenFunction(res -> {
                    JsonObject result = GsonStatic.fromJson(res.body(), JsonObject.class);

                    HttpReq.assertStatus(res);

                    String status = result.get("status").getAsString();
                    return "ok".equalsIgnoreCase(status);
                });
    }
}
