package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TmdbUtil {

    private static final String TMDB_API = "6bde7d268c5fd4b5baa41499612158e2";

    /**
     * 获取番剧在tmdb的名称
     *
     * @param name
     * @return
     */
    public synchronized static String getName(String name, String type) {
        name = name.trim();
        if (StrUtil.isBlank(name)) {
            return "";
        }
        AtomicReference<String> year = new AtomicReference<>("");
        if (ReUtil.contains(StringEnum.YEAR_REG, name)) {
            year.set(ReUtil.get(StringEnum.YEAR_REG, name, 1));
            name = name.replaceAll(StringEnum.YEAR_REG, "")
                    .trim();
        }
        if (StrUtil.isBlank(name)) {
            return "";
        }
        Config config = ConfigUtil.CONFIG;
        String tmdbLanguage = config.getTmdbLanguage();
        AtomicReference<String> tmdbId = new AtomicReference<>("");
        String themoviedbName;
        try {
            String finalName = name;
            themoviedbName = HttpReq.get("https://api.themoviedb.org/3/search/" + type, true)
                    .form("query", URLUtil.encodeBlank(name))
                    .form("api_key", TMDB_API)
                    .form("language", tmdbLanguage)
                    .thenFunction(res -> {
                        Assert.isTrue(res.isOk(), "status: {}", res.getStatus());
                        List<JsonObject> results = GsonStatic.fromJsonList(GsonStatic.fromJson(res.body(), JsonObject.class)
                                .getAsJsonArray("results"), JsonObject.class);
                        if (results.isEmpty()) {
                            if (!finalName.contains(" ")) {
                                return "";
                            }
                            ThreadUtil.sleep(500);
                            return getName(finalName.split(" ")[0], type);
                        }
                        JsonObject jsonObject = results.get(0);
                        String id = jsonObject.get("id").getAsString();
                        String title = Optional.of(jsonObject)
                                .map(o -> o.get("name"))
                                .orElse(jsonObject.get("title")).getAsString();

                        String date = Optional.of(jsonObject)
                                .map(o -> o.get("first_air_date"))
                                .orElse(jsonObject.get("release_date")).getAsString();

                        title = RenameUtil.getName(title);
                        tmdbId.set(id);

                        if (StrUtil.isNotBlank(year.get())) {
                            year.set(String.valueOf(DateUtil.year(DateUtil.parse(date))));
                        }

                        return StrUtil.blankToDefault(title, "");
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            return "";
        }
        if (StrUtil.isBlank(themoviedbName)) {
            return "";
        }
        if (StrUtil.isNotBlank(year.get())) {
            themoviedbName = StrFormatter.format("{} ({})", themoviedbName, year);
        }
        if (config.getTmdbId() && StrUtil.isNotBlank(tmdbId.get())) {
            themoviedbName = StrFormatter.format("{} [tmdbid={}]", themoviedbName, tmdbId.get());
        }

        return themoviedbName;
    }
}
