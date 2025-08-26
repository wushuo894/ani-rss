package ani.rss.notification;

import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Telegram
 */
@Slf4j
public class TelegramNotification implements BaseNotification {
    public static synchronized Map<String, String> getUpdates(NotificationConfig notificationConfig) {
        String telegramBotToken = notificationConfig.getTelegramBotToken();
        if (StrUtil.isBlank(telegramBotToken)) {
            return Map.of();
        }
        String telegramApiHost = notificationConfig.getTelegramApiHost();
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");
        String url = StrFormatter.format("{}/bot{}/getUpdates", telegramApiHost, telegramBotToken);
        Map<String, String> map = new HashMap<>();
        return HttpReq.get(url)
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonElement result = jsonObject.get("result");
                    if (Objects.isNull(result)) {
                        return map;
                    }
                    result.getAsJsonArray()
                            .asList()
                            .stream()
                            .map(JsonElement::getAsJsonObject)
                            .map(o -> o.getAsJsonObject("message"))
                            .filter(Objects::nonNull)
                            .map(o -> o.getAsJsonObject("chat"))
                            .filter(Objects::nonNull)
                            .forEach(o ->
                                    map.put(
                                            o.get("type").getAsString() + ": " + buildUsername(o),
                                            o.get("id").getAsString()
                                    )
                            );
                    return map;
                });
    }

    private static String buildUsername(JsonObject jsonObject) {
        if (jsonObject.has("username")) {
            return jsonObject.get("username").getAsString();
        }
        String firstName = Optional.ofNullable(jsonObject.get("first_name"))
                .map(JsonElement::getAsString)
                .orElse("");
        String lastName = Optional.ofNullable(jsonObject.get("last_name"))
                .map(JsonElement::getAsString)
                .orElse("");
        return firstName + " " + lastName;
    }

    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        notificationConfig = ObjectUtil.clone(notificationConfig);

        String telegramBotToken = notificationConfig.getTelegramBotToken();
        String telegramChatId = notificationConfig.getTelegramChatId();
        Integer telegramTopicId = notificationConfig.getTelegramTopicId();
        String telegramApiHost = notificationConfig.getTelegramApiHost();
        Boolean telegramImage = notificationConfig.getTelegramImage();
        String telegramFormat = notificationConfig.getTelegramFormat();

        if (StrUtil.isBlank(telegramChatId) || StrUtil.isBlank(telegramBotToken)) {
            log.warn("telegram 通知的参数不完整");
            return false;
        }
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");

        String notificationTemplate = replaceNotificationTemplate(ani, notificationConfig, text, notificationStatusEnum);

        if (!telegramImage) {
            String url = StrFormatter.format("{}/bot{}/sendMessage", telegramApiHost, telegramBotToken);

            // 未启用图片
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", telegramChatId);
            if (telegramTopicId > -1) {
                body.put("message_thread_id", telegramTopicId);
            }
            body.put("text", notificationTemplate);
            if (StrUtil.isNotBlank(telegramFormat)) {
                body.put("parse_mode", telegramFormat);
            }
            return HttpReq.post(url)
                    .body(GsonStatic.toJson(body))
                    .thenFunction(HttpResponse::isOk);
        }

        String cover = ani.getCover();
        if (StrUtil.isBlank(cover)) {
            notificationConfig.setTelegramImage(false);
            return send(notificationConfig, ani, text, notificationStatusEnum);
        }

        File configDir = ConfigUtil.getConfigDir();
        File photo = new File(configDir + "/files/" + cover);

        if (!photo.exists()) {
            notificationConfig.setTelegramImage(false);
            return send(notificationConfig, ani, text, notificationStatusEnum);
        }

        String url = StrFormatter.format("{}/bot{}/sendPhoto", telegramApiHost, telegramBotToken);

        HttpRequest request = HttpReq.post(url)
                .contentType(ContentType.MULTIPART.getValue())
                .form("chat_id", telegramChatId)
                .form("caption", notificationTemplate)
                .form("photo", photo)
                .form("parse_mode", telegramFormat);

        if (telegramTopicId > -1) {
            request.form("message_thread_id", telegramTopicId);
        }

        return request
                .thenFunction(HttpResponse::isOk);
    }
}
