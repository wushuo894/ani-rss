package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class TmdbUtil {

    private static final String TMDB_API = "6bde7d268c5fd4b5baa41499612158e2";

    /**
     * 获取番剧在tmdb的名称
     *
     * @param name
     * @param ani
     * @return
     */
    public synchronized static String getName(Ani ani) {
        String type = ani.getOva() ? "movie" : "tv";
        String name = ani.getTitle().trim();
        if (StrUtil.isBlank(name)) {
            return "";
        }

        boolean year = ReUtil.contains(StringEnum.YEAR_REG, name);

        if (year) {
            name = name.replaceAll(StringEnum.YEAR_REG, "")
                    .trim();
        }
        if (StrUtil.isBlank(name)) {
            return "";
        }
        Config config = ConfigUtil.CONFIG;
        Tmdb tmdb;
        try {
            tmdb = getTmdb(name, type);
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
            themoviedbName = StrFormatter.format("{} [tmdbid={}]", themoviedbName, tmdb.getId());
        }

        return themoviedbName;
    }

    public static Tmdb getTmdb(String titleName, String type) {
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();

        return HttpReq.get("https://api.themoviedb.org/3/search/" + type, true)
                .form("query", URLUtil.encodeBlank(titleName))
                .form("api_key", TMDB_API)
                .form("include_adult", "true")
                .form("language", tmdbLanguage)
                .thenFunction(res -> {
                    Assert.isTrue(res.isOk(), "status: {}", res.getStatus());
                    JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);

                    List<JsonObject> results =
                            GsonStatic.fromJsonList(body.getAsJsonArray("results"), JsonObject.class);

                    // 过滤出动漫 genreIds 16
                    results = results.stream()
                            .filter(it -> {
                                JsonElement genreIds = it.get("genre_ids");
                                if (Objects.isNull(genreIds) || genreIds.isJsonNull()) {
                                    return false;
                                }
                                return GsonStatic.fromJsonList(genreIds.getAsJsonArray(), Integer.class).contains(16);
                            }).toList();

                    if (results.isEmpty()) {
                        if (!titleName.contains(" ")) {
                            return null;
                        }
                        ThreadUtil.sleep(500);
                        return getTmdb(titleName.split(" ")[0], type);
                    }

                    List<Tmdb> tmdbList = results.stream()
                            .map(jsonObject -> {
                                String id = jsonObject.get("id").getAsString();
                                String title = Optional.of(jsonObject)
                                        .map(o -> o.get("name"))
                                        .orElse(jsonObject.get("title")).getAsString();

                                String date = Optional.of(jsonObject)
                                        .map(o -> o.get("first_air_date"))
                                        .orElse(jsonObject.get("release_date")).getAsString();

                                title = RenameUtil.getName(title);

                                return new Tmdb()
                                        .setId(id)
                                        .setName(title)
                                        .setDate(DateUtil.parse(date));
                            }).toList();

                    // 优先使用名称完全匹配
                    return tmdbList.stream()
                            .filter(tmdb -> tmdb.getName().equals(titleName))
                            .findFirst()
                            .orElse(tmdbList.get(0));
                });
    }

    @Data
    @Accessors(chain = true)
    public static class Tmdb implements Serializable {
        private String id;
        private String name;
        private Date date;
    }
}
