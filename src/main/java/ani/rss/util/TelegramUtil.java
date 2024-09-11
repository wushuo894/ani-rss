package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TelegramUtil {
    private static final Gson gson = new Gson();

    public static synchronized void send(Config config, String text) {
        String telegramBotToken = config.getTelegramBotToken();
        String telegramChatId = config.getTelegramChatId();
        if (StrUtil.isBlank(telegramChatId) || StrUtil.isBlank(telegramBotToken)) {
            log.warn("telegram 通知的参数不完整");
            return;
        }

        String url = StrFormatter.format("https://api.telegram.org/bot{}/sendMessage", telegramBotToken);
        HttpReq.post(url, true)
                .body(gson.toJson(Map.of(
                        "chat_id", telegramChatId,
                        "text", text
                )))
                .thenFunction(HttpResponse::isOk);
    }

    public static synchronized Map<String, String> getUpdates(Config config) {
        String telegramBotToken = config.getTelegramBotToken();
        if (StrUtil.isBlank(telegramBotToken)) {
            return Map.of();
        }
        String url = StrFormatter.format("https://api.telegram.org/bot{}/getUpdates", telegramBotToken);
        try {
            Map<String, String> map = new HashMap<>();
            return HttpReq.get(url, true)
                    .thenFunction(res -> {
                        JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                        jsonObject.get("result").getAsJsonArray()
                                .asList()
                                .stream()
                                .map(JsonElement::getAsJsonObject)
                                .map(o -> o.get("message").getAsJsonObject())
                                .map(o -> o.get("from").getAsJsonObject())
                                .forEach(o -> {
                                    map.put(o.get("username").getAsString(), o.get("id").getAsString());
                                });
                        return map;
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Map.of();
    }
}
