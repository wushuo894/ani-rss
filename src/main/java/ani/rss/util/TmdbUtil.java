package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Tmdb;
import ani.rss.entity.TmdbGroup;
import ani.rss.enums.StringEnum;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class TmdbUtil {
    public static String getTmdbApi() {
        Config config = ConfigUtil.CONFIG;
        String tmdbApi = config.getTmdbApi();
        tmdbApi = StrUtil.blankToDefault(tmdbApi, "https://api.themoviedb.org");
        return tmdbApi;
    }

    public static String getTmdbApiKey() {
        Config config = ConfigUtil.CONFIG;
        String tmdbApiKey = config.getTmdbApiKey();
        return StrUtil.blankToDefault(tmdbApiKey, "6bde7d268c5fd4b5baa41499612158e2");
    }

    /**
     * 获取番剧在tmdb的名称
     *
     * @param ani
     * @return
     */
    public synchronized static String getName(Ani ani) {
        String type = ani.getOva() ? "movie" : "tv";
        String name = ani.getTitle().trim();
        if (StrUtil.isBlank(name)) {
            return "";
        }

        Config config = ConfigUtil.CONFIG;

        boolean year = config.getTitleYear();
        if (year) {
            name = name.replaceAll(StringEnum.YEAR_REG, "")
                    .trim();
        }

        if (StrUtil.isBlank(name)) {
            return "";
        }

        Tmdb tmdb;
        try {
            tmdb = getTmdb(name, type);
            getRomaji(tmdb, type);
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            return "";
        }
        ani.setTmdb(tmdb);


        if (Objects.isNull(tmdb)) {
            return "";
        }

        String themoviedbName = tmdb.getName();
        if (StrUtil.isBlank(themoviedbName)) {
            return "";
        }

        if (year) {
            themoviedbName = StrFormatter.format("{} ({})", themoviedbName, DateUtil.year(tmdb.getDate()));
        }

        if (config.getTmdbId()) {
            if (config.getPlexTitleMode()) {
                themoviedbName = StrFormatter.format("{} {tmdb-{}}", themoviedbName, tmdb.getId());
            } else {
                themoviedbName = StrFormatter.format("{} [tmdbid={}]", themoviedbName, tmdb.getId());
            }
        }

        return themoviedbName;
    }

    /**
     * 获取罗马音
     *
     * @param tmdb
     * @param tmdbType
     */
    public static void getRomaji(Tmdb tmdb, String tmdbType) {
        if (Objects.isNull(tmdb)) {
            return;
        }

        Config config = ConfigUtil.CONFIG;
        Boolean tmdbRomaji = config.getTmdbRomaji();
        if (!tmdbRomaji) {
            // 未开启罗马音
            return;
        }

        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();
        String id = tmdb.getId();
        String url = StrFormatter.format("{}/3/{}/{}/alternative_titles", tmdbApi, tmdbType, id);
        HttpReq.get(url, true)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .then(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    List<JsonObject> results = GsonStatic.fromJsonList(jsonObject.getAsJsonArray("results"), JsonObject.class);
                    for (JsonObject result : results) {
                        String iso31661 = result.get("iso_3166_1").getAsString();
                        String type = result.get("type").getAsString();
                        String title = result.get("title").getAsString();
                        if (!iso31661.equals("JP")) {
                            continue;
                        }
                        if (List.of("romaji", "romanization").contains(type.toLowerCase())) {
                            // 判断为罗马音
                            tmdb.setName(title);
                            return;
                        }
                    }
                    String romaji = "";
                    try {
                        romaji = AniListUtil.getRomaji(tmdb.getName());
                    } catch (Exception e) {
                        log.error("通过AniList获取罗马音失败");
                        log.error(e.getMessage(), e);
                    }
                    if (StrUtil.isNotBlank(romaji)) {
                        tmdb.setName(romaji);
                    }
                });
    }

    public static Tmdb getTmdb(String titleName, String type) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        if (config.getPlexTitleMode()) {
            titleName = ReUtil.replaceAll(titleName, StringEnum.PLEX_TMDB_ID_REG, "")
                    .trim();
        } else {
            titleName = ReUtil.replaceAll(titleName, StringEnum.TMDB_ID_REG, "")
                    .trim();
        }
        titleName = ReUtil.replaceAll(titleName, StringEnum.YEAR_REG, "");
        titleName = titleName.trim();
        if (StrUtil.isBlank(titleName)) {
            return null;
        }

        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String finalTitleName = titleName;
        return HttpReq.get(tmdbApi + "/3/search/" + type, true)
                .timeout(5000)
                .form("query", URLUtil.encodeBlank(titleName))
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);

                    List<JsonObject> results =
                            GsonStatic.fromJsonList(body.getAsJsonArray("results"), JsonObject.class);

                    Boolean tmdbAnime = config.getTmdbAnime();

                    if (tmdbAnime) {
                        // 过滤出动漫 genreIds 16
                        results = results.stream()
                                .filter(it -> {
                                    JsonElement genreIds = it.get("genre_ids");
                                    if (Objects.isNull(genreIds) || genreIds.isJsonNull()) {
                                        return false;
                                    }
                                    return GsonStatic.fromJsonList(genreIds.getAsJsonArray(), Integer.class).contains(16);
                                }).toList();
                    }

                    if (results.isEmpty()) {
                        List<String> split = StrUtil.split(finalTitleName, " ", true, true);
                        if (split.size() < 2) {
                            return null;
                        }
                        ThreadUtil.sleep(500);

                        split.remove(split.size() - 1);
                        return getTmdb(CollUtil.join(split, " "), type);
                    }

                    List<Tmdb> tmdbList = results.stream()
                            .map(jsonObject -> {
                                String id = jsonObject.get("id").getAsString();
                                String title = Opt.of(jsonObject)
                                        .map(o -> o.get("name"))
                                        .orElse(jsonObject.get("title"))
                                        .getAsString();

                                String originalName = Opt.of(jsonObject)
                                        .map(o -> o.get("original_name"))
                                        .filter(Objects::nonNull)
                                        .map(JsonElement::getAsString)
                                        .orElse("");

                                String date = Opt.of(jsonObject)
                                        .map(o -> o.get("first_air_date"))
                                        .orElse(jsonObject.get("release_date"))
                                        .getAsString();

                                if (StrUtil.isBlank(date)) {
                                    return null;
                                }

                                title = RenameUtil.getName(title);

                                return new Tmdb()
                                        .setId(id)
                                        .setName(title)
                                        .setOriginalName(originalName)
                                        .setDate(DateUtil.parse(date))
                                        .setTmdbGroupId("");
                            })
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparingLong(tmdb -> Long.MAX_VALUE - tmdb.getDate().getTime()))
                            .toList();

                    // 优先使用名称完全匹配
                    return tmdbList.stream()
                            .filter(tmdb -> {
                                String name = tmdb.getName();
                                String originalName = tmdb.getOriginalName();
                                return List.of(name, originalName).contains(finalTitleName);
                            })
                            .findFirst()
                            .orElse(tmdbList.get(0));
                });
    }

    /**
     * 获取每集的标题
     *
     * @param ani
     * @return
     */
    public static synchronized Map<Integer, String> getEpisodeTitleMap(Ani ani) {
        Map<Integer, String> episodeTitleMap = new HashMap<>();

        if (Objects.isNull(ani)) {
            return episodeTitleMap;
        }

        Tmdb tmdb = ani.getTmdb();
        Integer season = ani.getSeason();
        Boolean ova = ani.getOva();

        if (ova) {
            return episodeTitleMap;
        }

        if (Objects.isNull(tmdb)) {
            return episodeTitleMap;
        }

        String tmdbId = tmdb.getId();
        String tmdbGroupId = tmdb.getTmdbGroupId();

        if (StrUtil.isBlank(tmdbId)) {
            return episodeTitleMap;
        }

        String key = StrFormatter.format("TMDB_getEpisodeTitleMap:{}:{}:{}", tmdbId, tmdbGroupId, season);

        Map<Integer, String> cacheMap = MyCacheUtil.get(key);
        if (Objects.nonNull(cacheMap)) {
            return cacheMap;
        }

        episodeTitleMap = getEpisodeTitleMap(tmdb, season);
        if (episodeTitleMap.isEmpty()) {
            MyCacheUtil.put(key, episodeTitleMap, 1000 * 10);
        } else {
            MyCacheUtil.put(key, episodeTitleMap, TimeUnit.MINUTES.toMillis(5));
        }
        return episodeTitleMap;
    }

    /**
     * 获取每集的标题
     *
     * @param tmdb
     * @param season
     * @return
     */
    public static Map<Integer, String> getEpisodeTitleMap(Tmdb tmdb, Integer season) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        Map<Integer, String> map = new HashMap<>();

        if (Objects.isNull(tmdb)) {
            return map;
        }

        Consumer<List<JsonObject>> episodesToMap = episodes -> {
            for (JsonObject episode : episodes) {
                int episodeNumber = episode.get("episode_number").getAsInt();
                if (episode.has("order")) {
                    episodeNumber = episode.get("order").getAsInt() + 1;
                }
                String name = episode.get("name").getAsString();
                name = RenameUtil.getName(name);
                if (map.containsKey(episodeNumber)) {
                    continue;
                }
                map.put(episodeNumber, name);
            }
        };

        try {
            String id = tmdb.getId();
            String tmdbGroupId = tmdb.getTmdbGroupId();
            String url = StrFormatter.format("{}/3/tv/{}/season/{}", tmdbApi, id, season);

            Config config = ConfigUtil.CONFIG;
            String tmdbLanguage = config.getTmdbLanguage();

            if (StrUtil.isNotBlank(tmdbGroupId)) {
                // 得到了剧集组的id
                HttpReq.get(tmdbApi + "/3/tv/episode_group/" + tmdbGroupId, true)
                        .timeout(5000)
                        .form("api_key", tmdbApiKey)
                        .form("include_adult", "true")
                        .form("language", tmdbLanguage)
                        .then(response -> {
                            int status = response.getStatus();
                            if (status == 404) {
                                return;
                            }
                            HttpReq.assertStatus(response);
                            JsonObject body = GsonStatic.fromJson(response.body(), JsonObject.class);
                            body.getAsJsonArray("groups")
                                    .asList()
                                    .stream()
                                    .map(JsonElement::getAsJsonObject)
                                    .filter(o -> o.get("order").getAsInt() == season)
                                    .map(o -> o.getAsJsonArray("episodes"))
                                    .map(JsonArray::asList)
                                    .map(o -> o.stream().map(JsonElement::getAsJsonObject).toList())
                                    .findFirst()
                                    .ifPresent(episodesToMap);
                        });
                return map;
            }

            HttpReq.get(url, true)
                    .timeout(5000)
                    .form("api_key", tmdbApiKey)
                    .form("include_adult", "true")
                    .form("language", tmdbLanguage)
                    .then(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            return;
                        }
                        HttpReq.assertStatus(res);
                        JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);
                        List<JsonObject> episodes = GsonStatic.fromJsonList(
                                body.getAsJsonArray("episodes"), JsonObject.class
                        );
                        episodesToMap.accept(episodes);
                    });
        } catch (Exception e) {
            log.error(ExceptionUtil.getMessage(e), e);
        }
        return map;
    }

    /**
     * 获取剧集组
     *
     * @param tmdb
     * @return
     */
    public static List<TmdbGroup> getTmdbGroup(Tmdb tmdb) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        String id = tmdb.getId();

        Map<Integer, String> typeMap = Map.of(
                1, "首播日期",
                2, "独立",
                3, "DVD",
                4, "数字",
                5, "故事线",
                6, "制片",
                7, "电视"
        );

        String url = StrFormatter.format("{}/3/tv/{}/episode_groups", tmdbApi, id);
        return HttpReq.get(url, true)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(response -> {
                    HttpReq.assertStatus(response);
                    JsonObject body = GsonStatic.fromJson(response.body(), JsonObject.class);
                    JsonArray results = body.getAsJsonArray("results");
                    return GsonStatic.fromJsonList(results, TmdbGroup.class)
                            .stream()
                            .peek(tmdbGroup -> {
                                Integer type = tmdbGroup.getType();
                                String typeName = typeMap.getOrDefault(type, "其他");
                                tmdbGroup.setTypeName(typeName);
                            }).toList();
                });
    }
}
