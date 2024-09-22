package ani.rss.util;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BgmUtil {
    private static final String host = "https://api.bgm.tv";
    private static final Gson gson = new Gson();

    /**
     * 查找番剧id
     *
     * @param name
     * @return
     */
    public static String getSubjectId(String name) {
        return HttpReq.get(host + "/search/subject/" + name, true)
                .form("type", 2)
                .form("responseGroup", "small")
                .thenFunction(res -> {
                    JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                    List<JsonElement> list = jsonObject.get("list").getAsJsonArray().asList();
                    if (list.isEmpty()) {
                        return "";
                    }

                    for (JsonElement jsonElement : list) {
                        JsonObject itemObject = jsonElement.getAsJsonObject();
                        String nameCn = itemObject.get("name_cn").getAsString();
                        if (nameCn.equalsIgnoreCase(name)) {
                            return itemObject.get("id").getAsString();
                        }
                    }
                    return list.get(0).getAsJsonObject().get("id").getAsString();
                });
    }

    /**
     * 收藏番剧
     *
     * @param subjectId
     */
    public static void collections(String subjectId) {
        Objects.requireNonNull(subjectId);
        HttpReq.post(host + "/v0/users/-/collections/" + subjectId, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .contentType(ContentType.JSON.getValue())
                .body(gson.toJson(Map.of("type", 3)))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 获取 EpisodeId
     *
     * @param subjectId
     * @param e
     * @return
     */
    public static String getEpisodeId(String subjectId, Double e) {
        Objects.requireNonNull(subjectId);
        return HttpReq.get(host + "/v0/episodes", true)
                .form("subject_id", subjectId)
                .thenFunction(res -> {
                    List<JsonElement> list = gson.fromJson(res.body(), JsonObject.class).get("data").getAsJsonArray().asList();
                    for (JsonElement jsonElement : list) {
                        JsonObject itemObject = jsonElement.getAsJsonObject();
                        double ep = itemObject.get("ep").getAsDouble();
                        if (ep == e) {
                            return itemObject.get("id").getAsString();
                        }
                    }
                    return "";
                });
    }

    /**
     * 标记为看过
     *
     * @param episodeId
     */
    public static void collectionsEpisodes(String episodeId) {
        Objects.requireNonNull(episodeId);
        HttpReq.put(host + "/v0/users/-/collections/-/episodes/" + episodeId, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .contentType(ContentType.JSON.getValue())
                .body(gson.toJson(Map.of("type", 2)))
                .thenFunction(res -> {
                    System.out.println(res.body());
                    return "";
                });
    }


}
