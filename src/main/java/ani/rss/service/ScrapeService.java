package ani.rss.service;

import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.tmdb.Tmdb;
import ani.rss.entity.tmdb.TmdbCredit;
import ani.rss.entity.tmdb.TmdbEpisode;
import ani.rss.entity.tmdb.TmdbSeason;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TmdbTypeEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.TmdbUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 刮削
 */
@Slf4j
public class ScrapeService {
    /**
     * 刮削
     *
     * @param ani
     * @param force
     */
    public static void scrape(Ani ani, Boolean force) {
        String title = ani.getTitle();

        Tmdb tmdb = ani.getTmdb();

        if (Objects.isNull(tmdb)) {
            return;
        }

        Boolean ova = ani.getOva();
        try {
            log.info("scrape ... {}", title);
            if (ova) {
                scrapeMovie(ani, force);
            } else {
                scrapeTv(ani, force);
            }
            log.info("scrape finish {}", title);
        } catch (Exception e) {
            log.error("scrape error {}", title);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 电影刮削
     *
     * @param ani
     * @param force
     * @throws Exception
     */
    public static void scrapeMovie(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        BeanUtil.copyProperties(
                TmdbUtil.getTmdb(tmdb, TmdbTypeEnum.MOVIE),
                tmdb,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );

        List<TmdbCredit> credits = TmdbUtil.getCredits(tmdb, TmdbTypeEnum.MOVIE);

        tmdb.setCredits(credits);

        File downloadPath = DownloadService.getDownloadPath(ani);

        if (!downloadPath.exists()) {
            return;
        }

        File[] files = downloadPath.listFiles();

        if (ArrayUtil.isEmpty(files)) {
            return;
        }

        Optional<File> first = Stream.of(files)
                .filter(file -> {
                    String extName = FileUtil.extName(file);
                    return BaseDownload.videoFormat.contains(extName);
                })
                .max(Comparator.comparingLong(File::length));

        if (first.isEmpty()) {
            return;
        }

        File file = first.get();
        String mainName = FileUtil.mainName(file);

        String outputPath = downloadPath + "/" + mainName + ".nfo";

        if (force || !FileUtil.exist(outputPath)) {
            NfoGenerator.generateMovieNfo(tmdb, outputPath);
        }

        File posterFile = new File(downloadPath + "/poster.jpg");
        File fanartFile = new File(downloadPath + "/fanart.jpg");
        String posterPath = tmdb.getPosterPath();
        String backdropPath = tmdb.getBackdropPath();

        if (force || !FileUtil.exist(posterFile)) {
            HttpReq.get("https://image.tmdb.org/t/p/w1280" + posterPath)
                    .then(res -> {
                        InputStream inputStream = res.bodyStream();
                        FileUtil.writeFromStream(inputStream, posterFile, true);
                    });
        }

        if (force || !FileUtil.exist(fanartFile)) {
            HttpReq.get("https://image.tmdb.org/t/p/w1280" + backdropPath)
                    .then(res -> {
                        InputStream inputStream = res.bodyStream();
                        FileUtil.writeFromStream(inputStream, fanartFile, true);
                    });
        }
    }

    /**
     * 电视剧刮削
     *
     * @param ani
     * @param force
     * @throws Exception
     */
    public static void scrapeTv(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        BeanUtil.copyProperties(
                TmdbUtil.getTmdb(tmdb, TmdbTypeEnum.TV),
                tmdb,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );

        List<TmdbCredit> credits = TmdbUtil.getCredits(tmdb, TmdbTypeEnum.TV);

        tmdb.setCredits(credits);

        File downloadPath = DownloadService.getDownloadPath(ani);

        if (!downloadPath.exists()) {
            return;
        }

        String tvShowNfoFile = downloadPath.getParent() + "/tvshow.nfo";

        if (force || !FileUtil.exist(tvShowNfoFile)) {
            NfoGenerator.generateTvShowNfo(tmdb, tvShowNfoFile);
        }

        File posterFile = new File(downloadPath.getParent() + "/poster.jpg");
        File fanartFile = new File(downloadPath.getParent() + "/fanart.jpg");
        String posterPath = tmdb.getPosterPath();
        String backdropPath = tmdb.getBackdropPath();

        if (force || !FileUtil.exist(posterFile)) {
            HttpReq.get("https://image.tmdb.org/t/p/w1280" + posterPath)
                    .then(res -> {
                        InputStream inputStream = res.bodyStream();
                        FileUtil.writeFromStream(inputStream, posterFile, true);
                    });
        }

        if (force || !FileUtil.exist(fanartFile)) {
            HttpReq.get("https://image.tmdb.org/t/p/w1280" + backdropPath)
                    .then(res -> {
                        InputStream inputStream = res.bodyStream();
                        FileUtil.writeFromStream(inputStream, fanartFile, true);
                    });
        }

        Integer season = ani.getSeason();

        Optional<TmdbSeason> optional = TmdbUtil.getTmdbSeason(tmdb, season);
        if (optional.isEmpty()) {
            return;
        }

        String seasonFormat = String.format("%02d", season);

        TmdbSeason tmdbSeason = optional.get();
        String seasonPosterPath = tmdbSeason.getPosterPath();
        seasonPosterPath = StrUtil.blankToDefault(seasonPosterPath, posterPath);

        File seasonPosterFile = new File(downloadPath.getParent() + "/season" + seasonFormat + "-poster.jpg");

        if (force || !FileUtil.exist(seasonPosterFile)) {
            HttpReq.get("https://image.tmdb.org/t/p/w1280" + seasonPosterPath)
                    .then(res -> {
                        InputStream inputStream = res.bodyStream();
                        FileUtil.writeFromStream(inputStream, seasonPosterFile, true);
                    });
        }

        String seasonNfoFile = downloadPath + "/season.nfo";
        if (force || !FileUtil.exist(seasonNfoFile)) {
            NfoGenerator.generateSeasonNfo(tmdbSeason, seasonNfoFile);
        }

        File[] files = downloadPath.listFiles();
        if (Objects.isNull(files)) {
            return;
        }

        Map<Integer, TmdbEpisode> episodeMap = tmdbSeason
                .getEpisodes()
                .stream()
                .collect(Collectors.toMap(TmdbEpisode::getEpisodeNumber, it -> it));

        for (File file : files) {
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }
            if (!BaseDownload.videoFormat.contains(extName)) {
                continue;
            }

            String mainName = FileUtil.mainName(file);

            if (!ReUtil.contains(StringEnum.SEASON_REG, mainName)) {
                continue;
            }

            Integer episodeNumber =
                    Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, mainName, 2));

            if (!episodeMap.containsKey(episodeNumber)) {
                continue;
            }

            TmdbEpisode tmdbEpisode = episodeMap.get(episodeNumber);

            String stillPath = tmdbEpisode.getStillPath();
            File thumbFile = new File(downloadPath + "/" + mainName + "-thumb.jpg");
            if (force || !FileUtil.exist(thumbFile)) {
                HttpReq.get("https://image.tmdb.org/t/p/w1280" + stillPath)
                        .then(res -> {
                            InputStream inputStream = res.bodyStream();
                            FileUtil.writeFromStream(inputStream, thumbFile, true);
                        });
            }

            String episodeFile = downloadPath + "/" + mainName + ".nfo";
            if (force || !FileUtil.exist(episodeFile)) {
                NfoGenerator.generateEpisodeNfo(tmdbEpisode, episodeFile);
            }

        }

    }

}
