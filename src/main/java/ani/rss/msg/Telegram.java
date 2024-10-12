package ani.rss.msg;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.ConfigUtil;
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

@Slf4j
public class Telegram implements Message {
    public Boolean send(Config config, Ani ani, String text) {
        String telegramBotToken = config.getTelegramBotToken();
        String telegramChatId = config.getTelegramChatId();
        String telegramApiHost = config.getTelegramApiHost();
        Boolean telegramImage = config.getTelegramImage();
        if (StrUtil.isBlank(telegramChatId) || StrUtil.isBlank(telegramBotToken)) {
            log.warn("telegram 通知的参数不完整");
            return false;
        }
        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");

        String url = StrFormatter.format("{}/bot{}/sendMessage", telegramApiHost, telegramBotToken);

        if (Objects.isNull(ani) || !telegramImage) {
            return HttpReq.post(url, true)
                    .body(gson.toJson(Map.of(
                            "chat_id", telegramChatId,
                            "text", text
                    )))
                    .thenFunction(HttpResponse::isOk);
        }
        String cover = ani.getCover();
        File configDir = ConfigUtil.getConfigDir();
        File photo = new File(configDir + "/files/" + cover);
        if (StrUtil.isBlank(cover) || !photo.exists()) {
            return send(config, null, text);
        }

        url = StrFormatter.format("{}/bot{}/sendPhoto", telegramApiHost, telegramBotToken);
        return HttpReq.post(url, true)
                .contentType(ContentType.MULTIPART.getValue())
                .form("chat_id", telegramChatId)
                .form("caption", text)
                .form("photo", photo)
                .thenFunction(HttpResponse::isOk);
    }

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
                    JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                    jsonObject.get("result").getAsJsonArray()
                            .asList()
                            .stream()
                            .map(JsonElement::getAsJsonObject)
                            .map(o -> o.getAsJsonObject("message"))
                            .filter(Objects::nonNull)
                            .map(o -> o.getAsJsonObject("chat"))
                            .filter(Objects::nonNull)
                            .forEach(o ->
                                    map.put(
                                            o.get("type").getAsString() + ": " + o.get("username").getAsString(),
                                            o.get("id").getAsString()
                                    )
                            );
                    return map;
                });
    }
}
