package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.MessageEnum;
import ani.rss.util.ConfigUtil;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
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
public class Telegram implements Message {
    public static synchronized Map<String, String> getUpdates(Config config) {
        String telegramBotToken = config.getTelegramBotToken();
        if (StrUtil.isBlank(telegramBotToken)) {
            return Map.of();
        }
        String telegramApiHost = config.getTelegramApiHost();
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");
        String url = StrFormatter.format("{}/bot{}/getUpdates", telegramApiHost, telegramBotToken);
        Map<String, String> map = new HashMap<>();
        return HttpReq.get(url, true)
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

    public Boolean send(Config config, Ani ani, String text, MessageEnum messageEnum) {
        text = replaceMessageTemplate(ani, config.getMessageTemplate(), text, messageEnum);
        String telegramBotToken = config.getTelegramBotToken();
        String telegramChatId = config.getTelegramChatId();
        String telegramTopicId = config.getTelegramTopicId();
        String telegramApiHost = config.getTelegramApiHost();
        Boolean telegramImage = config.getTelegramImage();
        String telegramFormat = config.getTelegramFormat();
        if (StrUtil.isBlank(telegramChatId) || StrUtil.isBlank(telegramBotToken)) {
            log.warn("telegram 通知的参数不完整");
            return false;
        }
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");

        String url = StrFormatter.format("{}/bot{}/sendMessage", telegramApiHost, telegramBotToken);

        if (Objects.isNull(ani) || !telegramImage) {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", telegramChatId);
            body.put("message_thread_id",telegramTopicId);
            body.put("text", text);
            if (StrUtil.isNotBlank(telegramFormat)) {
                body.put("parse_mode", telegramFormat);
            }
            return HttpReq.post(url, true)
                    .body(GsonStatic.toJson(body))
                    .thenFunction(HttpResponse::isOk);
        }
        String cover = ani.getCover();
        File configDir = ConfigUtil.getConfigDir();
        File photo = new File(configDir + "/files/" + cover);
        if (StrUtil.isBlank(cover) || !photo.exists()) {
            return send(config, null, text, messageEnum);
        }

        url = StrFormatter.format("{}/bot{}/sendPhoto", telegramApiHost, telegramBotToken);
        return HttpReq.post(url, true)
                .contentType(ContentType.MULTIPART.getValue())
                .form("chat_id", telegramChatId)
                .form("message_thread_id",telegramTopicId)
                .form("caption", text)
                .form("photo", photo)
                .form("parse_mode", telegramFormat)
                .thenFunction(HttpResponse::isOk);
    }
}
