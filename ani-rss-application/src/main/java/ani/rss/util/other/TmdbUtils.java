package ani.rss.util.other;

import ani.rss.commons.CacheUtils;
import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.CustomTmdbConfig;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import wushuo.tmdb.api.TmdbUtil;
import wushuo.tmdb.api.entity.*;
import wushuo.tmdb.api.enums.TmdbTypeEnum;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * tmdb封装
 */
@Slf4j
public class TmdbUtils {
    public final static TmdbConfig config = new CustomTmdbConfig();
    public final static TmdbUtil TMDB_UTIL = new TmdbUtil(config);

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

        Optional<Tmdb> tmdb;
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

        tmdb.ifPresent(ani::setTmdb);

        if (tmdb.isEmpty()) {
            return "";
        }

        String themoviedbName = tmdb.get().getName();
        return getFinalName(themoviedbName, tmdb.get());
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
        return TMDB_UTIL.getTitles(tmdb, tmdbType);
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
    public static Optional<Tmdb> getTmdbMovie(String titleName) {
        Optional<Tmdb> tmdb = getTmdb(titleName, TmdbTypeEnum.MOVIE);
        tmdb.ifPresent(it -> getRomaji(it, TmdbTypeEnum.MOVIE));
        return tmdb;
    }

    /**
     * 根据标题获得tmdb
     *
     * @param titleName 标题名
     * @return
     */
    public static Optional<Tmdb> getTmdbTv(String titleName) {
        Optional<Tmdb> tmdb = getTmdb(titleName, TmdbTypeEnum.TV);
        tmdb.ifPresent(it -> getRomaji(it, TmdbTypeEnum.TV));
        return tmdb;
    }

    /**
     * 根据名称获取tmdb信息
     *
     * @param titleName 标题名
     * @param tmdbType  类型
     * @return
     */
    public static Optional<Tmdb> getTmdb(String titleName, TmdbTypeEnum tmdbType) {
        return TMDB_UTIL.getTmdb(titleName, tmdbType);
    }

    /**
     * 获取季信息
     *
     * @param tmdb   tmdb
     * @param season 季
     * @return
     */
    public static Optional<TmdbSeason> getTmdbSeason(Tmdb tmdb, Integer season) {
        return TMDB_UTIL.getTmdbSeason(tmdb, season);
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
        return TMDB_UTIL.getEpisodeTitleMap(tmdb, season);
    }

    /**
     * 获取剧集组
     *
     * @param tmdb tmdb
     * @return
     */
    public static List<TmdbGroup> getTmdbGroup(Tmdb tmdb) {
        return TMDB_UTIL.getTmdbGroup(tmdb);
    }

    /**
     * 获取图片
     *
     * @param tmdb     tmdb
     * @param tmdbType 类型
     * @return
     */
    public static TmdbImages getTmdbImages(Tmdb tmdb, TmdbTypeEnum tmdbType) {
        return TMDB_UTIL.getTmdbImages(tmdb, tmdbType);
    }

    public static Optional<Tmdb> getTmdb(Tmdb tmdb, TmdbTypeEnum tmdbTypeEnum) {
        return TMDB_UTIL.getTmdb(tmdb, tmdbTypeEnum);
    }
}
