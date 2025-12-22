package ani.rss.util.other;

import ani.rss.commons.CacheUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.Config;
import ani.rss.enums.BgmTokenTypeEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import wushuo.tmdb.api.entity.Tmdb;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BGM
 */
@Slf4j
public class BgmUtil {
    private static final String host = "https://api.bgm.tv";

    /**
     * 获取bgm名称
     *
     * @param bgmInfo
     * @return
     */
    public static synchronized String getFinalName(BgmInfo bgmInfo) {
        Config config = ConfigUtil.CONFIG;

        Boolean bgmJpName = config.getBgmJpName();

        String name = bgmInfo.getName();
        String nameCn = bgmInfo.getNameCn();
        String title = StrUtil.blankToDefault(nameCn, name);

        if (bgmJpName) {
            title = name;
        }

        if (StrUtil.isBlank(title)) {
            title = "无标题";
        }

        return title.trim();
    }

    /**
     * 获取bgm名称
     *
     * @param bgmInfo
     * @param tmdb
     * @return
     */
    public static synchronized String getFinalName(BgmInfo bgmInfo, Tmdb tmdb) {
        Config config = ConfigUtil.CONFIG;
        Boolean titleYear = config.getTitleYear();

        String title = getFinalName(bgmInfo);

        Date date = bgmInfo.getDate();

        if (titleYear) {
            title = StrFormatter.format("{} ({})", title, DateUtil.year(date));
        }

        return TmdbUtils.getFinalName(title, tmdb);
    }

    /**
     * 搜索番剧
     *
     * @param name
     * @return
     */
    public static List<JsonObject> search(String name) {
        if (StrUtil.isBlank(name)) {
            return new ArrayList<>();
        }

        name = name.replace("1/2", "½");

        String url = UrlBuilder.of(host + "/search/subject/" + name)
                .addQuery("type", 2)
                .addQuery("max_results", 25)
                .addQuery("responseGroup", "small")
                .toString();

        HttpRequest httpRequest = HttpReq.get(url);

        return setToken(httpRequest)
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return new ArrayList<>();
                    }
                    String body = res.body();
                    if (!JSONUtil.isTypeJSON(body)) {
                        return new ArrayList<>();
                    }

                    JsonObject jsonObject = GsonStatic.fromJson(body, JsonObject.class);
                    JsonElement code = jsonObject.get("code");
                    if (Objects.nonNull(code)) {
                        if (code.getAsInt() == 404) {
                            return new ArrayList<>();
                        }
                    }
                    return jsonObject.get("list").getAsJsonArray()
                            .asList()
                            .stream().map(JsonElement::getAsJsonObject)
                            .toList();
                });
    }

    /**
     * 查找番剧id
     *
     * @param bgmName 名称
     * @param s       季度
     * @return 番剧id
     */
    public static synchronized String getSubjectId(String bgmName, Integer s) {
        if (StrUtil.isBlank(bgmName)) {
            return "";
        }

        String key = "BGM_getSubjectId:" + bgmName;

        if (CacheUtils.containsKey(key)) {
            return CacheUtils.get(key);
        }
        List<JsonObject> list = search(bgmName);
        if (list.isEmpty()) {
            return "";
        }

        String tempName = StrFormatter.format("{} 第{}季", bgmName, Convert.numberToChinese(s, false));

        String id = "";
        // 优先使用名称完全匹配的
        for (JsonObject itemObject : list) {
            String name = itemObject.get("name").getAsString();
            String nameCn = itemObject.get("name_cn").getAsString();

            if (s == 1) {
                if (List.of(name, nameCn).contains(bgmName)) {
                    id = itemObject.get("id").getAsString();
                    break;
                }
            }

            if (List.of(name, nameCn).contains(tempName)) {
                id = itemObject.get("id").getAsString();
                break;
            }
        }
        // 次之使用第一个
        if (StrUtil.isBlank(id)) {
            id = list.get(0).get("id").getAsString();
        }
        ThreadUtil.sleep(1000);
        CacheUtils.put(key, id, TimeUnit.MINUTES.toMillis(10));
        return id;
    }

    /**
     * 获取番剧id
     *
     * @param ani
     * @return
     */
    public static String getSubjectId(Ani ani) {
        String bgmUrl = ani.getBgmUrl();
        if (StrUtil.isBlank(bgmUrl) && "mikan".equals(ani.getType())) {
            String bangumiId = AniUtil.getBangumiId(ani);
            Assert.notBlank(bangumiId, "无法取得 bangumiId, {}", ani.getTitle());
            MikanUtil.getMikanInfo(ani, "");
            bgmUrl = ani.getUrl();
        }
        Assert.notBlank(bgmUrl, "bgmUrl 不能为空");
        String regStr = "^http(s)?://.+\\/(\\d+)(\\/)?$";
        Assert.isTrue(ReUtil.contains(regStr, bgmUrl));
        return ReUtil.get(regStr, bgmUrl, 2);
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
        HttpRequest httpRequest = HttpReq.get(host + "/v0/episodes");
        setToken(httpRequest);

        return httpRequest
                .form("subject_id", subjectId)
                .form("type", 0)
                .form("limit", 1000)
                .form("offset", 0)
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return List.of();
                    }

                    String body = res.body();
                    if (!JSONUtil.isTypeJSON(body)) {
                        return List.of();
                    }

                    return GsonStatic.fromJson(body, JsonObject.class)
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
                            .toList();
                });
    }

    public static JsonObject me() {
        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        Assert.notBlank(bgmToken, "BgmToken 未填写");

        String key = "BGM_me:" + bgmToken;

        String me = CacheUtils.get(key);
        if (StrUtil.isNotBlank(me)) {
            return GsonStatic.fromJson(me, JsonObject.class);
        }

        JsonObject jsonObject = setToken(HttpReq.get(host + "/v0/me"))
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), JsonObject.class);
                });

        CacheUtils.put(key, GsonStatic.toJson(jsonObject), TimeUnit.MINUTES.toMillis(10));
        return jsonObject;
    }

    /**
     * 获取用户名
     *
     * @return
     */
    public static String username() {
        JsonObject me = me();
        return Opt.of(me)
                .map(o -> o.get("username"))
                .filter(Objects::nonNull)
                .map(JsonElement::getAsString)
                .filter(StrUtil::isNotBlank)
                .orElse(String.valueOf(me.get("id").getAsInt()));
    }

    /**
     * 对番剧进行评分
     */
    public static Integer rate(String subjectId, Integer rate) {
        if (Objects.isNull(rate)) {
            // 获取评分
            String username = username();
            return setToken(HttpReq.get(host + "/v0/users/" + username + "/collections/" + subjectId))
                    .thenFunction(res -> {
                        if (res.getStatus() == 404) {
                            return 0;
                        }
                        HttpReq.assertStatus(res);
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        return jsonObject.get("rate").getAsInt();
                    });
        }

        setToken(HttpReq.post(host + "/v0/users/-/collections/" + subjectId))
                .contentType(ContentType.JSON.getValue())
                .body(GsonStatic.toJson(Map.of(
                        "type", 3,
                        "rate", rate
                )))
                .then(HttpReq::assertStatus);
        return rate;
    }

    /**
     * 收藏番剧
     *
     * @param subjectId 番剧id
     */
    public static void collections(String subjectId) {
        Assert.notBlank(subjectId, "subjectId 不能为空");

        String key = "BGM_collections:" + subjectId;
        if (CacheUtils.containsKey(key)) {
            return;
        }
        CacheUtils.put(key, subjectId, TimeUnit.MINUTES.toMillis(5));

        String username = username();

        // 如果已经订阅，则不再订阅
        Boolean ok = setToken(HttpReq.get(host + "/v0/users/" + username + "/collections/" + subjectId))
                .thenFunction(HttpResponse::isOk);

        if (ok) {
            // 已经收藏
            log.info("已收藏番剧: {}", subjectId);
            return;
        }

        setToken(HttpReq.post(host + "/v0/users/-/collections/" + subjectId))
                .contentType(ContentType.JSON.getValue())
                .body(GsonStatic.toJson(Map.of("type", 3)))
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
        String epId = "";
        String sortId = "";

        String key = "BGM_getEpisodeId:" + subjectId;

        List<JsonObject> episodes = CacheUtils.get(key);
        if (Objects.isNull(episodes)) {
            episodes = getEpisodes(subjectId, 0);
            CacheUtils.put(key, episodes, TimeUnit.MINUTES.toMillis(10));
        }
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

        // bgm点格子前先判断状态，防止刷屏 #142
        JsonObject jsonObject = setToken(HttpReq.get(host + "/v0/users/-/collections/-/episodes/" + episodeId))
                .contentType(ContentType.JSON.getValue())
                .thenFunction(res -> GsonStatic.fromJson(res.body(), JsonObject.class));

        int typeNow = jsonObject.get("type").getAsInt();
        if (type == typeNow) {
            return;
        }

        // 间隔 500 毫秒, 防止流控
        ThreadUtil.sleep(500);

        setToken(HttpReq.put(host + "/v0/users/-/collections/-/episodes/" + episodeId))
                .contentType(ContentType.JSON.getValue())
                .body(GsonStatic.toJson(Map.of("type", type)))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 获取对应的bgm信息
     *
     * @param ani
     * @param isCache
     * @return
     */
    public static BgmInfo getBgmInfo(Ani ani, Boolean isCache) {
        String subjectId = getSubjectId(ani);
        Assert.notBlank(subjectId, "无法取得 subjectId, {}", ani.getTitle());
        return getBgmInfo(subjectId, isCache);
    }

    /**
     * 获取对应的bgm信息
     *
     * @param ani
     * @return
     */
    public static BgmInfo getBgmInfo(Ani ani) {
        String subjectId = getSubjectId(ani);
        return getBgmInfo(subjectId);
    }

    /**
     * 获取对应的bgm信息
     *
     * @param subjectId
     * @return
     */
    public static BgmInfo getBgmInfo(String subjectId) {
        return getBgmInfo(subjectId, false);
    }

    /**
     * 获取对应的bgm信息
     *
     * @param subjectId
     * @param isCache
     * @return
     */
    public static BgmInfo getBgmInfo(String subjectId, Boolean isCache) {
        Function<HttpResponse, BgmInfo> fun = res -> {
            HttpReq.assertStatus(res);
            String body = res.body();
            Assert.isTrue(JSONUtil.isTypeJSON(body), "no json");
            JsonObject jsonObject = GsonStatic.fromJson(body, JsonObject.class);
            BgmInfo bgmInfo = new BgmInfo();

            String name = jsonObject.get("name").getAsString();
            String nameCn = jsonObject.get("name_cn").getAsString();

            String platform = jsonObject.get("platform").getAsString();
            int eps = jsonObject.get("eps").getAsInt();

            double score = 0.0;
            JsonObject rating = jsonObject.getAsJsonObject("rating");
            if (Objects.nonNull(rating)) {
                score = rating.get("score").getAsDouble();
            }
            bgmInfo
                    .setSubjectId(subjectId)
                    .setNameCn(RenameUtil.getName(nameCn))
                    .setName(RenameUtil.getName(name))
                    .setEps(eps)
                    .setScore(score)
                    .setOva(List.of("OVA", "剧场版").contains(platform.toUpperCase()));

            JsonElement date = jsonObject.get("date");

            if (Objects.nonNull(date) && !date.isJsonNull()) {
                bgmInfo.setDate(
                        DateUtil.parse(date.getAsString(), DatePattern.NORM_DATE_PATTERN)
                );
            } else {
                bgmInfo.setDate(new Date());
            }


            JsonObject images = jsonObject.getAsJsonObject("images");
            if (Objects.nonNull(images)) {
                Config config = ConfigUtil.CONFIG;
                String bgmImage = config.getBgmImage();
                bgmInfo.setImage(images.get(bgmImage).getAsString());
            }

            Set<String> tags = jsonObject.getAsJsonArray("tags")
                    .asList()
                    .stream()
                    .map(JsonElement::getAsJsonObject)
                    .map(o -> o.get("name").getAsString())
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toSet());

            int season = 1;


            // 从标签获取季
            for (String tag : tags) {
                String seasonReg = StrFormatter.format("第({}+)季", ReUtil.RE_CHINESE);
                if (!ReUtil.contains(seasonReg, tag)) {
                    continue;
                }
                try {
                    season = Convert.chineseToNumber(ReUtil.get(seasonReg, tag, 1));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            // 从中文标题获取季
            String seasonReg = StrFormatter.format("第({}+)季", ReUtil.RE_CHINESE);
            if (ReUtil.contains(seasonReg, nameCn)) {
                try {
                    season = Convert.chineseToNumber(ReUtil.get(seasonReg, nameCn, 1));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            // 从原标题获取季
            seasonReg = "[Ss]eason ?(\\d+)";
            if (ReUtil.contains(seasonReg, name)) {
                try {
                    season = Integer.parseInt(ReUtil.get(seasonReg, name, 1));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            bgmInfo.setSeason(season);
            return bgmInfo;
        };

        if (!isCache) {
            // 不使用缓存
            HttpRequest httpRequest = HttpReq.get(host + "/v0/subjects/" + subjectId);
            return setToken(httpRequest).thenFunction(fun);
        }

        AtomicReference<BgmInfo> bgmInfoAR = new AtomicReference<>();
        AtomicReference<BgmInfo> bgmInfoCacheAR = new AtomicReference<>();

        // 并行获取bgm信息
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    // 不使用缓存
                    HttpRequest httpRequest = HttpReq.get(host + "/v0/subjects/" + subjectId);
                    try {
                        BgmInfo bgmInfo = setToken(httpRequest)
                                .thenFunction(fun);
                        bgmInfoAR.set(bgmInfo);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    HttpRequest httpRequest = HttpReq
                            .get("https://bgm-cache.wushuo.top/" + subjectId.charAt(0) + "/" + subjectId + ".json");
                    try {
                        BgmInfo bgmInfo = httpRequest
                                .thenFunction(fun);
                        bgmInfoCacheAR.set(bgmInfo);
                    } catch (Exception ignored) {
                    }
                })
        ).join();

        BgmInfo bgmInfo = bgmInfoAR.get();

        bgmInfo = ObjectUtil.defaultIfNull(bgmInfo, bgmInfoCacheAR.get());

        Assert.notNull(bgmInfo, "获取 bgmInfo 失败!");

        return bgmInfo;
    }

    /**
     * 设置token
     *
     * @param httpRequest
     * @return
     */
    public static synchronized HttpRequest setToken(HttpRequest httpRequest) {
        Config config = ConfigUtil.CONFIG;

        String bgmToken = config.getBgmToken();

        if (StrUtil.isNotBlank(bgmToken)) {
            httpRequest.header(Header.AUTHORIZATION, "Bearer " + bgmToken);
        }

        ThreadUtil.sleep(RandomUtil.randomInt(500, 1000));
        return httpRequest;
    }

    /**
     * 获取剩余过期时间 单位: 天
     *
     * @return
     */
    public static Long getExpiresDays() {
        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            return 0L;
        }
        long expires = HttpReq.post("https://bgm.tv/oauth/token_status")
                .form("access_token", bgmToken)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return jsonObject.get("expires").getAsLong() * 1000L;
                });

        long currentTimeMillis = System.currentTimeMillis();

        long days = 0;

        if (expires > currentTimeMillis) {
            days = TimeUnit.MILLISECONDS.toDays(expires - currentTimeMillis);
        }
        return days;
    }

    /**
     * 刷新token
     */
    public static synchronized void refreshToken() {
        Config config = ConfigUtil.CONFIG;

        BgmTokenTypeEnum bgmTokenType = config.getBgmTokenType();
        if (bgmTokenType != BgmTokenTypeEnum.AUTO) {
            return;
        }

        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            return;
        }

        long days = getExpiresDays();

        if (days >= 3) {
            return;
        }

        String bgmAppID = config.getBgmAppID();
        String bgmAppSecret = config.getBgmAppSecret();
        String bgmRefreshToken = config.getBgmRefreshToken();
        String bgmRedirectUri = config.getBgmRedirectUri();

        if (StrUtil.isBlank(bgmAppID)) {
            return;
        }
        if (StrUtil.isBlank(bgmAppSecret)) {
            return;
        }
        if (StrUtil.isBlank(bgmRefreshToken)) {
            return;
        }
        if (StrUtil.isBlank(bgmRedirectUri)) {
            return;
        }

        HttpReq.post("https://bgm.tv/oauth/access_token")
                .body(GsonStatic.toJson(Map.of(
                        "grant_type", "refresh_token",
                        "client_id", bgmAppID,
                        "client_secret", bgmAppSecret,
                        "refresh_token", bgmRefreshToken,
                        "redirect_uri", bgmRedirectUri
                )))
                .then(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    String accessToken = jsonObject.get("access_token").getAsString();
                    String refreshToken = jsonObject.get("refresh_token").getAsString();
                    config.setBgmToken(accessToken)
                            .setBgmRefreshToken(refreshToken);
                });

        ConfigUtil.sync();
        log.info("BgmToken已自动刷新");
    }

    /**
     * 获取每集的标题
     *
     * @param ani
     * @return
     */
    public static synchronized Map<Integer, Function<Boolean, String>> getEpisodeTitleMap(Ani ani) {
        Map<Integer, Function<Boolean, String>> episodeTitleMap = new HashMap<>();

        if (Objects.isNull(ani)) {
            return episodeTitleMap;
        }

        String subjectId = getSubjectId(ani);

        if (StrUtil.isBlank(subjectId)) {
            return episodeTitleMap;
        }

        if (ani.getOva()) {
            return episodeTitleMap;
        }

        String key = "BGM_getEpisodeTitleMap:" + subjectId;

        Map<Integer, Function<Boolean, String>> cacheMap = CacheUtils.get(key);
        if (Objects.nonNull(cacheMap)) {
            return cacheMap;
        }

        try {
            List<JsonObject> data = getEpisodes(subjectId, 0);
            for (JsonObject it : data) {
                int ep = it.get("ep").getAsInt();
                String jpTitle = it.get("name").getAsString();

                String title = it.get("name_cn").getAsString();
                title = StrUtil.blankToDefault(title, it.get("name").getAsString());

                title = RenameUtil.getName(title);
                jpTitle = RenameUtil.getName(jpTitle);

                String defaultEpisodeTitle = "第" + ep + "集";

                title = StrUtil.blankToDefault(title, defaultEpisodeTitle);
                jpTitle = StrUtil.blankToDefault(jpTitle, defaultEpisodeTitle);

                AtomicReference<String> titleRef = new AtomicReference<>(title);
                AtomicReference<String> jpTitleRef = new AtomicReference<>(jpTitle);

                episodeTitleMap.put(ep, jp -> jp ? jpTitleRef.get() : titleRef.get());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        CacheUtils.put(key, episodeTitleMap, TimeUnit.MINUTES.toMillis(5));
        return episodeTitleMap;
    }

    /**
     * 获取集数 排除ova
     *
     * @param bgmInfo
     * @return
     */
    public static Integer getEps(BgmInfo bgmInfo) {
        int eps = bgmInfo.getEps();
        String subjectId = bgmInfo.getSubjectId();
        if (eps > 0) {
            try {
                eps = BgmUtil.getEpisodes(subjectId, 0).size();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return eps;
    }

    /**
     * bgm转ani
     *
     * @param bgmInfo
     * @param ani
     * @return
     */
    public static Ani toAni(BgmInfo bgmInfo, Ani ani) {
        String title = BgmUtil.getFinalName(bgmInfo);

        int eps = getEps(bgmInfo);

        String image = bgmInfo.getImage();

        Date date = bgmInfo.getDate();

        Config config = ConfigUtil.CONFIG;
        // 使用tmdb标题
        Boolean tmdb = config.getTmdb();

        ani
                .setBgmUrl("https://bgm.tv/subject/" + bgmInfo.getSubjectId())
                // 标题
                .setTitle(title)
                .setJpTitle(bgmInfo.getName())
                // 季
                .setSeason(bgmInfo.getSeason())
                // 总集数
                .setTotalEpisodeNumber(eps)
                // 剧场版
                .setOva(bgmInfo.getOva())
                // 评分
                .setScore(bgmInfo.getScore())
                // 年
                .setYear(DateUtil.year(date))
                // 月
                .setMonth(DateUtil.month(date) + 1)
                // 日
                .setDate(DateUtil.dayOfMonth(date))
                // 图片http地址
                .setImage(image)
                // 本地图片地址
                .setCover(AniUtil.saveJpg(image));

        // 获取tmdb标题
        String themoviedbName = TmdbUtils.getFinalName(ani);

        // 是否使用tmdb标题
        if (StrUtil.isNotBlank(themoviedbName) && tmdb) {
            ani
                    .setTitle(themoviedbName);
        } else {
            title = BgmUtil.getFinalName(bgmInfo, ani.getTmdb());
            ani.setTitle(title);
        }

        // 下载位置
        String downloadPath = DownloadService.getDownloadPath(ani);

        String alistPath = config.getAlistPath();
        String completedPathTemplate = config.getCompletedPathTemplate();

        Boolean ova = ani.getOva();
        if (ova) {
            // 剧场版默认不开启摸鱼检测
            ani.setProcrastinating(false);
            alistPath = config.getAlistOvaPath();
        }

        return ani
                // tmdb 标题
                .setThemoviedbName(themoviedbName)
                .setAlistPath(alistPath)
                .setDownloadPath(downloadPath)
                .setCustomCompletedPathTemplate(completedPathTemplate);
    }


}
