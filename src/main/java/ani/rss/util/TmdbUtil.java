package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TmdbUtil {

    /**
     * 获取番剧在tmdb的名称
     *
     * @param name
     * @return
     */
    public synchronized static String getName(String name) {
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
        Boolean tmdbTw = config.getTmdbTw();
        String acceptLanguage = "zh-CN";
        if (tmdbTw) {
            acceptLanguage = "zh-TW";
        }

        AtomicReference<String> tmdbId = new AtomicReference<>("");
        String themoviedbName;
        try {
            String finalName = name;
            themoviedbName = HttpReq.get("https://www.themoviedb.org/search", true)
                    .form("query", name)
                    .cookie("")
                    .header("accept-language", acceptLanguage)
                    .thenFunction(res -> {
                        org.jsoup.nodes.Document document = Jsoup.parse(res.body());
                        Element element = document.selectFirst(".title h2");
                        if (Objects.isNull(element)) {
                            if (!finalName.contains(" ")) {
                                return "";
                            }
                            ThreadUtil.sleep(500);
                            return getName(finalName.split(" ")[0]);
                        }
                        Element releaseDate = document.selectFirst(".release_date");
                        if (Objects.nonNull(releaseDate) && StrUtil.isNotBlank(year.get())) {
                            String s = "((19|20)\\d{2}) 年";
                            String text = releaseDate.text();
                            if (ReUtil.contains(s, text)) {
                                year.set(ReUtil.get(s, text, 1));
                            }
                        }
                        String title = element.ownText();
                        title = RenameUtil.getName(title);

                        Element a = document.selectFirst(".title a");
                        if (Objects.nonNull(a)) {
                            String href = a.attr("href");
                            tmdbId.set(ReUtil.get("\\d+", href, 0));
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
