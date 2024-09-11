package ani.rss.msg;

import ani.rss.entity.Config;
import ani.rss.util.HttpReq;
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
public class Telegram implements Message{
    private static final Gson gson = new Gson();

    public Boolean send(Config config, String text) {
        String telegramBotToken = config.getTelegramBotToken();
        String telegramChatId = config.getTelegramChatId();
        String telegramApiHost = config.getTelegramApiHost();
        if (StrUtil.isBlank(telegramChatId) || StrUtil.isBlank(telegramBotToken)) {
            log.warn("telegram 通知的参数不完整");
            return false;
        }

        telegramApiHost = StrUtil.blankToDefault(telegramApiHost, "https://api.telegram.org");

        String url = StrFormatter.format("{}/bot{}/sendMessage", telegramApiHost, telegramBotToken);
        return HttpReq.post(url, true)
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
                            .map(o -> o.get("message").getAsJsonObject())
                            .map(o -> o.get("from").getAsJsonObject())
                            .forEach(o -> {
                                map.put(o.get("username").getAsString(), o.get("id").getAsString());
                            });
                    return map;
                });
    }
}
