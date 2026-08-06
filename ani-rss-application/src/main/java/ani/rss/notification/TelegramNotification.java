package ani.rss.notification;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.web.ContentType;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

/**
 * Telegram
 */
@Slf4j
public class TelegramNotification implements BaseNotification {

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

    public static List<Message.Chat> getUpdates(NotificationConfig notificationConfig) {
        String telegramBotToken = notificationConfig.getTelegramBotToken();
        if (StrUtil.isBlank(telegramBotToken)) {
            return new ArrayList<>();
        }
        String telegramApiHost = notificationConfig.getTelegramApiHost();
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");
        String url = StrFormatter.format("{}/bot{}/getUpdates", telegramApiHost, telegramBotToken);
        return HttpReq.get(url)
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray result = jsonObject.getAsJsonArray("result");
                    List<Message.Chat> chatList = GsonStatic.fromJsonList(result, Update.class)
                            .stream()
                            .map(Update::getMessage)
                            .filter(Objects::nonNull)
                            .map(Message::getChat)
                            .filter(Objects::nonNull)
                            .peek(chat -> {
                                String username = chat.getUsername();
                                if (StrUtil.isNotBlank(username)) {
                                    return;
                                }
                                String firstName = chat.getFirstName();
                                String lastName = chat.getLastName();
                                username = StrUtil.join(" ", firstName, lastName);
                                chat.setUsername(username);
                            })
                            .toList();
                    return CollUtil.distinct(chatList, Message.Chat::getId, false);
                });
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
            return HttpReq.post(url, body)
                    .thenFunction(HttpResponse::isOk);
        }

        String cover = ani.getCover();
        if (StrUtil.isBlank(cover)) {
            notificationConfig.setTelegramImage(false);
            return send(notificationConfig, ani, text, notificationStatusEnum);
        }

        File configDir = ConfigUtil.getConfigDir();
        File photo = Path.of(configDir.toString(), "files", cover).toFile();

        if (!photo.exists()) {
            notificationConfig.setTelegramImage(false);
            return send(notificationConfig, ani, text, notificationStatusEnum);
        }

        String url = StrFormatter.format("{}/bot{}/sendPhoto", telegramApiHost, telegramBotToken);

        HttpRequest request = HttpReq.post(url)
                .contentType(ContentType.MULTIPART)
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

    @Data
    @Accessors(chain = true)
    private static class Update implements Serializable {
        private String updateId;
        @SerializedName(value = "message", alternate = "my_chat_member")
        private Message message;
    }

    @Data
    @Accessors(chain = true)
    public static class Message implements Serializable {
        @SerializedName(value = "messageId", alternate = "message_id")
        private Integer messageId;
        private Long date;
        private String text;
        private From from;
        private Chat chat;

        @Data
        @Accessors(chain = true)
        public static class From implements Serializable {
            private Integer id;
            @SerializedName(value = "isBot", alternate = "is_bot")
            private Boolean isBot;
            @SerializedName(value = "firstName", alternate = "first_name")
            private String firstName;
            @SerializedName(value = "lastName", alternate = "last_name")
            private String lastName;
            private String username;
            @SerializedName(value = "languageCode", alternate = "language_code")
            private String language_code;
        }

        @Data
        @Accessors(chain = true)
        public static class Chat implements Serializable {
            private Integer id;
            @SerializedName(value = "firstName", alternate = "first_name")
            private String firstName;
            @SerializedName(value = "lastName", alternate = "last_name")
            private String lastName;
            private String username;
            private String type;
        }
    }
}
