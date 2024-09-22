package ani.rss.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BGM
 */
public class BgmUtil {
    private static final String host = "https://api.bgm.tv";
    private static final Gson gson = new Gson();

    /**
     * 查找番剧id
     *
     * @param name 名称
     * @return 番剧id
     */
    public static String getSubjectId(String name) {
        return HttpReq.get(host + "/search/subject/" + name, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .form("type", 2)
                .form("responseGroup", "small")
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return "";
                    }
                    String body = res.body();
                    if (!JSONUtil.isTypeJSON(body)) {
                        return "";
                    }

                    JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
                    if (Objects.nonNull(jsonObject.get("code"))) {
                        if (jsonObject.get("code").getAsInt() == 404) {
                            return "";
                        }
                    }
                    List<JsonElement> list = jsonObject.get("list").getAsJsonArray().asList();
                    if (list.isEmpty()) {
                        return "";
                    }

                    // 优先使用名称完全匹配的
                    for (JsonElement jsonElement : list) {
                        JsonObject itemObject = jsonElement.getAsJsonObject();
                        String nameCn = itemObject.get("name_cn").getAsString();
                        if (nameCn.equalsIgnoreCase(name)) {
                            return itemObject.get("id").getAsString();
                        }
                    }
                    // 次之使用第一个
                    return list.get(0).getAsJsonObject().get("id").getAsString();
                });
    }

    /**
     * 收藏番剧
     *
     * @param subjectId 番剧id
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
     * @param subjectId 番剧id
     * @param e         集数
     * @return 集id
     */
    public static String getEpisodeId(String subjectId, Double e) {
        Objects.requireNonNull(subjectId);
        return HttpReq.get(host + "/v0/episodes", true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .form("subject_id", subjectId)
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return "";
                    }

                    String body = res.body();
                    if (!JSONUtil.isTypeJSON(body)) {
                        return "";
                    }

                    List<JsonElement> list = gson.fromJson(body, JsonObject.class).get("data").getAsJsonArray().asList();
                    String epId = "";
                    String sortId = "";
                    for (JsonElement jsonElement : list) {
                        JsonObject itemObject = jsonElement.getAsJsonObject();

                        int type = itemObject.get("type").getAsInt();
                        if (type > 0) {
                            continue;
                        }

                        double ep = itemObject.get("ep").getAsDouble();
                        double sort = itemObject.get("sort").getAsDouble();
                        if (ep == e) {
                            epId = itemObject.get("id").getAsString();
                            break;
                        }
                        if (sort == e) {
                            sortId = itemObject.get("id").getAsString();
                            break;
                        }
                    }
                    return StrUtil.blankToDefault(epId, sortId);
                });
    }

    /**
     * 标记
     *
     * @param episodeId 集id
     * @param type      0 未看过, 1 想看, 2 看过
     */
    public static void collectionsEpisodes(String episodeId, Integer type) {
        Objects.requireNonNull(episodeId);
        HttpReq.put(host + "/v0/users/-/collections/-/episodes/" + episodeId, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .contentType(ContentType.JSON.getValue())
                .body(gson.toJson(Map.of("type", type)))
                .thenFunction(HttpResponse::isOk);
    }


}
