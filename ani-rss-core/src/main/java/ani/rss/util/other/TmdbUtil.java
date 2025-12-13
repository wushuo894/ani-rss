package ani.rss.util.other;

import ani.rss.commons.CacheUtils;
import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.tmdb.*;
import ani.rss.enums.TmdbTypeEnum;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

/**
 * tmdb封装
 */
@Slf4j
public class TmdbUtil {
    /**
     * api
     *
     * @return
     */
    public static String getTmdbApi() {
        Config config = ConfigUtil.CONFIG;
        String tmdbApi = config.getTmdbApi();
        tmdbApi = StrUtil.blankToDefault(tmdbApi, "https://api.themoviedb.org");
        return tmdbApi;
    }

    /**
     * apiKey
     *
     * @return
     */
    public static String getTmdbApiKey() {
        Config config = ConfigUtil.CONFIG;
        String tmdbApiKey = config.getTmdbApiKey();
        return StrUtil.blankToDefault(tmdbApiKey, "450e4f651e1c93e31383e20f8e731e5f");
    }

    /**
     * 获取番剧在tmdb的名称
     *
     * @param ani 订阅
     * @return
     */
    public synchronized static String getFinalName(Ani ani) {
        Boolean ova = ani.getOva();
        String name = ani.getTitle();
        name = RenameUtil.renameDel(name, false);
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
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            return "";
        }
        ani.setTmdb(tmdb);

        if (Objects.isNull(tmdb)) {
            return "";
        }

        String themoviedbName = tmdb.getName();
        return getFinalName(themoviedbName, tmdb);
    }

    /**
     * 获取添加tmdbid与年份后的名称
     *
     * @param title 标题
     * @param tmdb  tmdb
     * @return
     */
    public static String getFinalName(String title, Tmdb tmdb) {
        if (Objects.isNull(tmdb)) {
            return title;
        }
        Config config = ConfigUtil.CONFIG;

        boolean titleYear = config.getTitleYear();
        if (titleYear) {
            title = RenameUtil.renameDel(title, false);
            title = StrFormatter.format("{} ({})", title, DateUtil.year(tmdb.getDate()));
        }

        boolean tmdbId = config.getTmdbId();
        boolean tmdbIdPlexMode = config.getTmdbIdPlexMode();
        if (tmdbId) {
            if (tmdbIdPlexMode) {
                title = StrFormatter.format("{} {tmdb-{}}", title, tmdb.getId());
            } else {
                title = StrFormatter.format("{} [tmdbid={}]", title, tmdb.getId());
            }
        }
        return RenameUtil.getName(title);
    }

    /**
     * 获取所有标题
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static List<TmdbTitle> getTitles(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        if (Objects.isNull(tmdb)) {
            return List.of();
        }

        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();
        String id = tmdb.getId();
        String url = StrFormatter.format("{}/3/{}/{}/alternative_titles", tmdbApi, tmdbType.getValue(), id);
        List<TmdbTitle> tmdbTitles = HttpReq.get(url)
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

        for (TmdbTitle tmdbTitle : tmdbTitles) {
            String name = RenameUtil.getName(tmdbTitle.getTitle());
            tmdbTitle.setTitle(name);
        }

        return tmdbTitles;
    }

    /**
     * 获取罗马音
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
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
                title = RenameUtil.getName(title);
                // 判断为罗马音
                tmdb.setName(title);
                return;
            }
        }

        String romaji = "";
        try {
            romaji = AniListUtil.getRomaji(tmdb.getName());
            romaji = RenameUtil.getName(romaji);
        } catch (Exception e) {
            log.error("通过AniList获取罗马音失败");
            log.error(e.getMessage(), e);
        }
        if (StrUtil.isNotBlank(romaji)) {
            tmdb.setName(romaji);
        }
    }

    /**
     * 根据标题获得tmdb
     *
     * @param titleName 标题名
     * @return
     */
    public static Tmdb getTmdbMovie(String titleName) {
        Tmdb tmdb = getTmdb(titleName, TmdbTypeEnum.MOVIE);
        getRomaji(tmdb, TmdbTypeEnum.MOVIE);
        return tmdb;
    }

    /**
     * 根据标题获得tmdb
     *
     * @param titleName 标题名
     * @return
     */
    public static Tmdb getTmdbTv(String titleName) {
        Tmdb tmdb = getTmdb(titleName, TmdbTypeEnum.TV);
        getRomaji(tmdb, TmdbTypeEnum.TV);
        return tmdb;
    }

    /**
     * 刷新tmdb信息
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static Tmdb getTmdb(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String id = tmdb.getId();

        String url = StrFormatter.format("{}/3/{}/{}", tmdbApi, tmdbType.getValue(), id);

        Tmdb newTmdb = HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .form("append_to_response", "translations,credits")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), Tmdb.class)
                            .setTmdbType(tmdbType);
                });

        BeanUtil.copyProperties(
                newTmdb,
                tmdb,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );

        // 宣传语
        String tagLine = tmdb.getTagline();

        if (StrUtil.isBlank(tagLine)) {
            TmdbTranslations translations = tmdb.getTranslations();
            Optional<String> first = translations
                    .getTranslations()
                    .stream()
                    .sorted(Comparator.comparingInt(it -> {
                        int i = List.of("CN", "TW", "HK", "JP", "US").indexOf(it.getIso31661());
                        if (i > -1) {
                            return i;
                        }
                        return Integer.MAX_VALUE;
                    }))
                    .map(TmdbTranslation::getData)
                    .map(TmdbTranslationData::getTagline)
                    .filter(StrUtil::isNotBlank)
                    .findFirst();
            if (first.isPresent()) {
                tagLine = first.get();
            }
        }

        // 宣传片
        List<TmdbVideo> videos = getVideos(tmdb, tmdbType);

        return tmdb
                .setTagline(tagLine)
                .setVideos(videos);
    }

    /**
     * 根据名称获取tmdb信息
     *
     * @param titleName 标题名
     * @param tmdbType  类型
     * @return
     */
    public static Tmdb getTmdb(String titleName, TmdbTypeEnum tmdbType) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        titleName = RenameUtil.renameDel(titleName, false);

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
                    Tmdb get = tmdbList.stream()
                            .filter(tmdb -> {
                                String name = tmdb.getName();
                                String originalName = tmdb.getOriginalName();
                                return List.of(name, originalName).contains(finalTitleName);
                            })
                            .findFirst()
                            .orElse(tmdbList.get(0));
                    String name = get.getName();
                    name = RenameUtil.getName(name);
                    get.setName(name);
                    return get;
                });
    }

    /**
     * 获取季信息
     *
     * @param tmdb   tmdb
     * @param season 季
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
                episode
                        .setSeasonNumber(season)
                        .setEpisodeNumber(order + 1);
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
     * @param ani 订阅
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

        Map<Integer, String> cacheMap = CacheUtils.get(key);
        if (Objects.nonNull(cacheMap)) {
            return cacheMap;
        }

        episodeTitleMap = getEpisodeTitleMap(tmdb, season);
        if (episodeTitleMap.isEmpty()) {
            CacheUtils.put(key, episodeTitleMap, 1000 * 10);
        } else {
            CacheUtils.put(key, episodeTitleMap, TimeUnit.MINUTES.toMillis(5));
        }
        return episodeTitleMap;
    }

    /**
     * 获取每集的标题
     *
     * @param tmdb   tmdb
     * @param season 季
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
                name = RenameUtil.getName(name);
                map.put(episodeNumber, name);
            }
        } catch (Exception e) {
            log.error(ExceptionUtils.getMessage(e), e);
        }
        return map;
    }

    /**
     * 获取剧集组
     *
     * @param tmdb tmdb
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
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static List<TmdbCredits> getCredits(Tmdb tmdb, TmdbTypeEnum tmdbType) {
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

                    return GsonStatic.fromJsonList(cast, TmdbCredits.class);
                });
    }

    /**
     * 获取图片
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static TmdbImages getTmdbImages(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String id = tmdb.getId();

        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        String url = StrFormatter.format("{}/3/{}/{}/images", tmdbApi, tmdbType.getValue(), id);
        TmdbImages tmdbImages = HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), TmdbImages.class);
                });

        // 排序方法
        ToDoubleFunction<TmdbImage> sortedFun = it -> {
            Double voteAverage = it.getVoteAverage();

            String iso6391 = it.getIso6391();
            String iso31661 = it.getIso31661();

            if (StrUtil.isBlank(iso6391)) {
                return 50 + voteAverage;
            }

            String lang = iso6391 + "-" + iso31661;

            if (tmdbLanguage.equals(lang)) {
                return voteAverage;
            }

            if (lang.equals("zh-CN")) {
                return 10 + voteAverage;
            }

            if (lang.startsWith("zh-")) {
                return 20 + voteAverage;
            }

            if (lang.startsWith("ja-")) {
                return 30 + voteAverage;
            }

            return 40 + voteAverage;
        };

        List<TmdbImage> logos = tmdbImages.getLogos();
        List<TmdbImage> posters = tmdbImages.getPosters();
        List<TmdbImage> backdrops = tmdbImages.getBackdrops();

        // 图片排序
        logos = logos.stream()
                .sorted(Comparator.comparingDouble(sortedFun))
                .toList();
        posters = posters.stream()
                .sorted(Comparator.comparingDouble(sortedFun))
                .toList();
        backdrops = backdrops.stream()
                .sorted(Comparator.comparingDouble(sortedFun))
                .toList();

        return tmdbImages.setLogos(logos)
                .setPosters(posters)
                .setBackdrops(backdrops);
    }

    /**
     * 获取预告片
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static List<TmdbVideo> getVideos(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        String tmdbApi = getTmdbApi();
        String tmdbApiKey = getTmdbApiKey();

        String id = tmdb.getId();

        String url = StrFormatter.format("{}/3/{}/{}/videos", tmdbApi, tmdbType.getValue(), id);
        return HttpReq.get(url)
                .timeout(5000)
                .form("api_key", tmdbApiKey)
                .form("include_adult", "true")
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonArray results = GsonStatic.fromJson(res.body(), JsonObject.class)
                            .getAsJsonArray("results");
                    return GsonStatic.fromJsonList(results, TmdbVideo.class);
                });
    }

}
