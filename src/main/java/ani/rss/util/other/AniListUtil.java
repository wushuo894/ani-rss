package ani.rss.util.other;

import ani.rss.util.basic.GsonStatic;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AniListUtil {
    /**
     * 获取罗马音
     *
     * @param title
     * @return
     */
    public static String getRomaji(String title) {
        if (StrUtil.isBlank(title)) {
            return "";
        }

        String body = StrFormatter.format("""
                {
                    "query": "query ($search: String) { Page (page: 1, perPage: 1) { media (search: $search, type: ANIME) { title { romaji native } } } }",
                    "variables": {
                        "search": "{}"
                    }
                }
                """, title);

        return HttpReq.post("https://graphql.anilist.co")
                .timeout(5000)
                .body(body)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray media = jsonObject.getAsJsonObject("data")
                            .getAsJsonObject("Page")
                            .getAsJsonArray("media");
                    if (media.isEmpty()) {
                        return "";
                    }
                    return media.get(0)
                            .getAsJsonObject()
                            .getAsJsonObject("title")
                            .get("romaji").getAsString();
                });
    }
}
