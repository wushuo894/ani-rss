package ani.rss.util.other;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.tmdb.*;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TmdbTypeEnum;
import ani.rss.util.basic.ExceptionUtil;
import ani.rss.util.basic.GsonStatic;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.basic.MyCacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
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
        Boolean ova = ani.getOva();
        String name = ani.getTitle().trim();
        if (StrUtil.isBlank(name)) {
            return "";
        }

        Config config = ConfigUtil.CONFIG;

        boolean titleYear = config.getTitleYear();
        name = name.replaceAll(StringEnum.YEAR_REG, "")
                .trim();

        if (StrUtil.isBlank(name)) {
            return "";
        }

        Tmdb tmdb;
        try {
            if (ova) {
                tmdb = getTmdbMovie(name);
            } else {
                tmdb = getTmdbTv(name);
            }
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

        themoviedbName = RenameUtil.getName(themoviedbName);

        if (StrUtil.isBlank(themoviedbName)) {
            return "";
        }

        if (titleYear) {
            themoviedbName = StrFormatter.format("{} ({})", themoviedbName, DateUtil.year(tmdb.getDate()));
        }
        return getName(themoviedbName, tmdb);
    }

    public static String getName(String title, Tmdb tmdb) {
        if (Objects.isNull(tmdb)) {
            return title;
        }
        Config config = ConfigUtil.CONFIG;
        Boolean tmdbId = config.getTmdbId();
        Boolean tmdbIdPlexMode = config.getTmdbIdPlexMode();
        if (tmdbId) {
            if (tmdbIdPlexMode) {
                title = StrFormatter.format("{} {tmdb-{}}", title, tmdb.getId());
            } else {
                title = StrFormatter.format("{} [tmdbid={}]", title, tmdb.getId());
            }
        }
        return title;
    }

    public static List<TmdbTitle> getTitles(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        if (Objects.isNull(tmdb)) {
            return List.of();
        }

        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();
        String id = tmdb.getId();
        String url = StrFormatter.format("{}/3/{}/{}/alternative_titles", tmdbApi, tmdbType.getValue(), id);
        return HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return GsonStatic.fromJsonList(
                            jsonObject.getAsJsonArray("results"),
                            TmdbTitle.class);
                });
    }

    /**
     * 获取罗马音
     *
     * @param tmdb
     * @param tmdbType
     */
    public static void getRomaji(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        if (Objects.isNull(tmdb)) {
            return;
        }

        Config config = ConfigUtil.CONFIG;
        Boolean tmdbRomaji = config.getTmdbRomaji();
        if (!tmdbRomaji) {
            // 未开启罗马音
            return;
        }

        List<TmdbTitle> titles = getTitles(tmdb, tmdbType);

        for (TmdbTitle tmdbTitle : titles) {
            String iso31661 = tmdbTitle.getIso31661();
            String type = tmdbTitle.getType();
            String title = tmdbTitle.getTitle();
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
    }

    public static Tmdb getTmdbMovie(String titleName) {
        Tmdb tmdb = getTmdb(titleName, TmdbTypeEnum.MOVIE);
        getRomaji(tmdb, TmdbTypeEnum.MOVIE);
        return tmdb;
    }

    public static Tmdb getTmdbTv(String titleName) {
        Tmdb tmdb = getTmdb(titleName, TmdbTypeEnum.TV);
        getRomaji(tmdb, TmdbTypeEnum.TV);
        return tmdb;
    }

    public static Tmdb getTmdb(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String id = tmdb.getId();

        String url = StrFormatter.format("{}/3/{}/{}", tmdbApi, tmdbType.getValue(), id);

        return HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), Tmdb.class)
                            .setTmdbType(tmdbType);
                });
    }

    public static Tmdb getTmdb(String titleName, TmdbTypeEnum tmdbType) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        titleName = ReUtil.replaceAll(titleName, StringEnum.TMDB_ID_REG, "")
                .trim();
        titleName = ReUtil.replaceAll(titleName, StringEnum.YEAR_REG, "");
        titleName = titleName.trim();
        if (StrUtil.isBlank(titleName)) {
            return null;
        }

        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String finalTitleName = titleName;
        return HttpReq.get(tmdbApi + "/3/search/" + tmdbType.getValue())
                .timeout(5000)
                .form("query", URLUtil.encodeBlank(titleName))
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);

                    List<Tmdb> tmdbs = new ArrayList<>();


                    for (JsonElement item : body.getAsJsonArray("results")) {
                        try {
                            Tmdb tmdb = GsonStatic.fromJson(item, Tmdb.class);
                            tmdb.setTmdbType(tmdbType);
                            tmdbs.add(tmdb);
                        } catch (Exception ignored) {
                        }
                    }

                    Boolean tmdbAnime = config.getTmdbAnime();

                    if (tmdbAnime) {
                        // 过滤出动漫 genreIds 16
                        tmdbs = tmdbs.stream()
                                .filter(it -> {
                                    List<Integer> genreIds = it.getGenreIds();
                                    return genreIds.contains(16);
                                }).toList();
                    }

                    if (tmdbs.isEmpty()) {
                        List<String> split = StrUtil.split(finalTitleName, " ", true, true);
                        if (split.size() < 2) {
                            return null;
                        }
                        ThreadUtil.sleep(500);

                        split.remove(split.size() - 1);
                        return getTmdb(CollUtil.join(split, " "), tmdbType);
                    }

                    List<Tmdb> tmdbList = tmdbs.stream()
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
     * 获取季信息
     *
     * @param tmdb
     * @param season
     * @return
     */
    public static Optional<TmdbSeason> getTmdbSeason(Tmdb tmdb, Integer season) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        String id = tmdb.getId();

        String tmdbApi = getTmdbApi();

        String tmdbApiKey = getTmdbApiKey();

        String tmdbGroupId = tmdb.getTmdbGroupId();

        if (StrUtil.isBlank(tmdbGroupId)) {
            String url = StrFormatter.format("{}/3/tv/{}/season/{}", tmdbApi, id, season);

            return HttpReq.get(url)
                    .timeout(5000)
                    .form("api_key", tmdbApiKey)
                    .form("include_adult", "true")
                    .form("language", tmdbLanguage)
                    .thenFunction(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            return Optional.empty();
                        }
                        HttpReq.assertStatus(res);
                        TmdbSeason tmdbSeason = GsonStatic.fromJson(res.body(), TmdbSeason.class);
                        return Optional.of(tmdbSeason);
                    });
        }

        String url = StrFormatter.format("{}/3/tv/episode_group/{}", tmdbApi, tmdbGroupId);

        Optional<JsonObject> jsonObject = HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    int status = res.getStatus();
                    if (status == 404) {
                        return Optional.empty();
                    }
                    HttpReq.assertStatus(res);
                    return Optional.of(
                            GsonStatic.fromJson(res.body(), JsonObject.class)
                    );
                });

        if (jsonObject.isEmpty()) {
            return Optional.empty();
        }

        JsonArray groups = jsonObject
                .get()
                .getAsJsonArray("groups");

        List<TmdbSeason> tmdbSeasons = GsonStatic.fromJsonList(groups, TmdbSeason.class);

        Optional<TmdbSeason> first = tmdbSeasons
                .stream()
                .filter(it -> it.getOrder().intValue() == season)
                .findFirst();
        first.ifPresent(tmdbSeason -> {
            List<TmdbEpisode> episodes = tmdbSeason.getEpisodes();
            for (TmdbEpisode episode : episodes) {
                Integer order = episode.getOrder();
                episode.setEpisodeNumber(order + 1);
            }

            Date airDate = episodes
                    .get(0)
                    .getAirDate();

            tmdbSeason
                    .setSeasonNumber(season)
                    .setOverview("")
                    .setVoteAverage(0.0)
                    .setAirDate(airDate);
        });

        return first;
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
        Map<Integer, String> map = new HashMap<>();

        if (Objects.isNull(tmdb)) {
            return map;
        }
        try {
            Optional<TmdbSeason> tmdbSeason = getTmdbSeason(tmdb, season);
            if (tmdbSeason.isEmpty()) {
                return map;
            }
            List<TmdbEpisode> episodes = tmdbSeason.get()
                    .getEpisodes();
            for (TmdbEpisode episode : episodes) {
                Integer episodeNumber = episode.getEpisodeNumber();
                String name = episode.getName();
                map.put(episodeNumber, name);
            }
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
        return HttpReq.get(url)
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

    /**
     * 演职人员
     *
     * @param tmdb
     * @param tmdbType
     * @return
     */
    public static List<TmdbCredit> getCredits(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        String id = tmdb.getId();

        String url = StrFormatter.format("{}/3/{}/{}/credits", tmdbApi, tmdbType.getValue(), id);
        return HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);

                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray cast = jsonObject.getAsJsonArray("cast");

                    return GsonStatic.fromJsonList(cast, TmdbCredit.class);
                });
    }

    /**
     * 获取图片
     *
     * @param tmdb
     * @param tmdbType
     * @return
     */
    public static TmdbImages getTmdbImages(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String id = tmdb.getId();

        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        String url = StrFormatter.format("{}/3/{}/{}/images", tmdbApi, tmdbType.getValue(), id);
        return HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), TmdbImages.class);
                });
    }

}
