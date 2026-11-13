package ani.rss.notification;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.web.ContentType;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
     * 渲染通知模板，根据图片开关发送文本或封面与正文组成的图文消息。
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

        // 关闭图片或没有可用封面时保留文本请求格式，兼容只具有 im 权限的旧配置。
        Object content = toMessage(notificationConfig, ani, host, apiKey, message);
        return HttpReq.post(host + "/api/v1/im/message", Map.of("umo", umo, "message", content))
                .header("Authorization", "Bearer " + apiKey)
                .contentType(ContentType.JSON)
                .thenFunction(res -> {
                    JsonObject result = GsonStatic.fromJson(res.body(), JsonObject.class);

                    HttpReq.assertStatus(res);

                    String status = result.get("status").getAsString();
                    return "ok".equalsIgnoreCase(status);
                });
    }

    /**
     * 将通知转换为文本或图文消息，图片仅使用配置目录内缓存的可读封面。
     *
     * @param notificationConfig 通知配置
     * @param ani                订阅
     * @param host               AstrBot 服务地址
     * @param apiKey             AstrBot API Key
     * @param message            渲染后的通知正文
     * @return 文本或 AstrBot 消息段列表
     */
    private Object toMessage(NotificationConfig notificationConfig, Ani ani, String host, String apiKey, String message) {
        String cover = ani.getCover();
        if (!notificationConfig.getAstrBotImage() || StrUtil.isBlank(cover)) {
            return message;
        }

        File configDir = ConfigUtil.getConfigDir();
        File photo = Path.of(configDir.toString(), "files", cover).toFile();

        if (!photo.exists()) {
            // 封面缺失时仍发送正文，不修改用户保存的图片开关。
            return message;
        }

        // 先上传封面获取附件 ID，再将图片与正文放在同一次消息请求中发送。
        String attachmentId = uploadCover(host, apiKey, photo);
        return List.of(
                Map.of("type", "image", "attachment_id", attachmentId),
                Map.of("type", "plain", "text", message)
        );
    }

    /**
     * 使用 multipart 表单上传封面，所用密钥需要具有 file 权限。
     *
     * @param host   AstrBot 服务地址
     * @param apiKey AstrBot API Key
     * @param photo  本地缓存的封面文件
     * @return 上传后的附件 ID
     */
    private String uploadCover(String host, String apiKey, File photo) {
        return HttpReq.post(host + "/api/v1/file")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(ContentType.MULTIPART)
                .form("file", photo)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);

                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return jsonObject.getAsJsonObject("data")
                            .get("attachment_id").getAsString();
                });
    }
}
