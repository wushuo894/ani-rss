package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.BigInfo;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * BGM
 */
public class BgmUtil {
    private static final String host = "https://api.bgm.tv";
    private static final Gson gson = new Gson();
    private static final Cache<String, String> nameCache = CacheUtil.newFIFOCache(64);

    /**
     * 查找番剧id
     *
     * @param name 名称
     * @return 番剧id
     */
    public static String getSubjectId(String name) {
        if (StrUtil.isBlank(name)) {
            return "";
        }

        if (nameCache.containsKey(name)) {
            return nameCache.get(name);
        }
        HttpRequest httpRequest = HttpReq.get(host + "/search/subject/" + name, true);

        setToken(httpRequest);

        String id = httpRequest
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
        ThreadUtil.sleep(1000);
        nameCache.put(name, id, TimeUnit.DAYS.toDays(1));
        return id;
    }

    /**
     * 收藏番剧
     *
     * @param subjectId 番剧id
     */
    public static void collections(String subjectId) {
        ThreadUtil.sleep(500);
        Objects.requireNonNull(subjectId);
        HttpReq.post(host + "/v0/users/-/collections/" + subjectId, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .contentType(ContentType.JSON.getValue())
                .body(gson.toJson(Map.of("type", 3)))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 获取视频列表
     *
     * @param subjectId 番剧id
     * @param type      0正常 1番外
     * @return
     */
    public static List<JsonObject> getEpisodes(String subjectId, Integer type) {
        ThreadUtil.sleep(500);
        Objects.requireNonNull(subjectId);
        HttpRequest httpRequest = HttpReq.get(host + "/v0/episodes", true);
        setToken(httpRequest);

        return httpRequest
                .form("subject_id", subjectId)
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return List.of();
                    }

                    String body = res.body();
                    if (!JSONUtil.isTypeJSON(body)) {
                        return List.of();
                    }

                    return gson.fromJson(body, JsonObject.class)
                            .get("data")
                            .getAsJsonArray()
                            .asList()
                            .stream()
                            .map(JsonElement::getAsJsonObject)
                            .filter(itemObject -> {
                                if (Objects.nonNull(type)) {
                                    return type == itemObject.get("type").getAsInt();
                                }
                                return true;
                            })
                            .collect(Collectors.toList());
                });
    }

    /**
     * 获取 EpisodeId
     *
     * @param subjectId 番剧id
     * @param e         集数
     * @return 集id
     */
    public static String getEpisodeId(String subjectId, Double e) {
        String epId = "";
        String sortId = "";

        List<JsonObject> episodes = getEpisodes(subjectId, 0);
        for (JsonObject itemObject : episodes) {
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
    }

    /**
     * 标记
     *
     * @param episodeId 集id
     * @param type      0 未看过, 1 想看, 2 看过
     */
    public static void collectionsEpisodes(String episodeId, Integer type) {
        ThreadUtil.sleep(500);
        Objects.requireNonNull(episodeId);
        HttpReq.put(host + "/v0/users/-/collections/-/episodes/" + episodeId, true)
                .header("Authorization", "Bearer " + ConfigUtil.CONFIG.getBgmToken())
                .contentType(ContentType.JSON.getValue())
                .body(gson.toJson(Map.of("type", type)))
                .thenFunction(HttpResponse::isOk);
    }

    public static BigInfo getBgmInfo(Ani ani) {
        String bgmUrl = ani.getBgmUrl();
        if (StrUtil.isBlank(bgmUrl)) {
            String bangumiId = AniUtil.getBangumiId(ani);
            Assert.notBlank(bangumiId);
            MikanUtil.getMikanInfo(ani, "");
            bgmUrl = ani.getUrl();
        }
        Assert.notBlank(bgmUrl);
        String regStr = "^http(s)?://.+\\/(\\d+)(\\/)?$";
        Assert.isTrue(ReUtil.contains(regStr, bgmUrl));
        String subjectId = ReUtil.get(regStr, bgmUrl, 2);
        return getBgmInfo(subjectId);
    }

    public static BigInfo getBgmInfo(String subjectId) {
        HttpRequest httpRequest = HttpReq.get(host + "/v0/subjects/" + subjectId, true);
        setToken(httpRequest);
        return httpRequest
                .thenFunction(res -> {
                    Assert.isTrue(res.isOk(), "status: {}", res.getStatus());
                    String body = res.body();
                    Assert.isTrue(JSONUtil.isTypeJSON(body), "no json");
                    JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
                    BigInfo bigInfo = new BigInfo();

                    String nameCn = jsonObject.get("name_cn").getAsString();
                    String date = jsonObject.get("date").getAsString();
                    String platform = jsonObject.get("platform").getAsString();
                    int eps = jsonObject.get("eps").getAsInt();

                    double score = 0.0;
                    JsonObject rating = jsonObject.getAsJsonObject("rating");
                    if (Objects.nonNull(rating)) {
                        score = rating.get("score").getAsDouble();
                    }
                    bigInfo
                            .setSubjectId(subjectId)
                            .setNameCn(nameCn)
                            .setDate(LocalDateTimeUtil.parse(date, DatePattern.NORM_DATE_PATTERN))
                            .setEps(eps)
                            .setScore(score)
                            .setOva("OVA".equalsIgnoreCase(platform));

                    JsonObject images = jsonObject.getAsJsonObject("images");
                    if (Objects.nonNull(images)) {
                        bigInfo.setImage(images.get("large").getAsString());
                    }
                    return bigInfo;
                });
    }

    public static void setToken(HttpRequest httpRequest) {
        String bgmToken = ConfigUtil.CONFIG.getBgmToken();
        if (StrUtil.isNotBlank(bgmToken)) {
            httpRequest.header("Authorization", "Bearer " + bgmToken);
        }
    }


}
