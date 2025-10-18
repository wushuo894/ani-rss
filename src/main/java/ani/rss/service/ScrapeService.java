package ani.rss.service;

import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.tmdb.*;
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
     * @param ani   订阅
     * @param force 强制
     */
    public static void scrape(Ani ani, Boolean force) {
        String title = ani.getTitle();

        Tmdb tmdb = ani.getTmdb();

        if (Objects.isNull(tmdb)) {
            return;
        }

        Boolean ova = ani.getOva();
        try {
            log.info("正在刮削 ... {}", title);
            if (ova) {
                scrapeMovie(ani, force);
            } else {
                scrapeTv(ani, force);
            }
            log.info("刮削完成 {}", title);
        } catch (Exception e) {
            log.error("刮削错误 {}", title);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 电影刮削
     *
     * @param ani   订阅
     * @param force 强制
     * @throws Exception
     */
    public static void scrapeMovie(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        // 更新tmdb信息
        BeanUtil.copyProperties(
                TmdbUtil.getTmdb(tmdb, TmdbTypeEnum.MOVIE),
                tmdb,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );

        // 演职人员
        List<TmdbCredit> credits = TmdbUtil.getCredits(tmdb, TmdbTypeEnum.MOVIE);
        tmdb.setCredits(credits);

        // 下载位置
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
            // 找不到视频文件
            return;
        }

        File file = first.get();
        String mainName = FileUtil.mainName(file);

        // 保存nfo
        String outputPath = downloadPath + "/" + mainName + ".nfo";
        if (force || !FileUtil.exist(outputPath)) {
            NfoGenerator.generateMovieNfo(tmdb, outputPath);
        }

        String posterPath = tmdb.getPosterPath();
        String fanartPath = tmdb.getBackdropPath();

        String posterExtName = FileUtil.extName(posterPath);
        String fanartExtName = FileUtil.extName(fanartPath);

        File posterFile = new File(downloadPath + "/poster." + posterExtName);
        File fanartFile = new File(downloadPath + "/fanart." + fanartExtName);

        // 封面、背景图
        saveImages(posterPath, posterFile, force);
        saveImages(fanartPath, fanartFile, force);

        TmdbImages tmdbImages = TmdbUtil.getTmdbImages(tmdb, TmdbTypeEnum.MOVIE);
        List<TmdbImage> logos = tmdbImages.getLogos();
        if (logos.isEmpty()) {
            return;
        }

        // 保存logo
        TmdbImage tmdbImage = logos.get(0);
        String logoPath = tmdbImage.getFilePath();
        String extName = FileUtil.extName(logoPath);
        File logoFile = new File(downloadPath + "/clearlogo." + extName);
        saveImages(logoPath, logoFile, force);
    }

    /**
     * 电视剧刮削
     *
     * @param ani   订阅
     * @param force 强制
     * @throws Exception
     */
    public static void scrapeTv(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        // 更新tmdb信息
        BeanUtil.copyProperties(
                TmdbUtil.getTmdb(tmdb, TmdbTypeEnum.TV),
                tmdb,
                CopyOptions
                        .create()
                        .setIgnoreNullValue(true)
        );

        // 演职人员
        List<TmdbCredit> credits = TmdbUtil.getCredits(tmdb, TmdbTypeEnum.TV);
        tmdb.setCredits(credits);

        // 下载位置
        File downloadPath = DownloadService.getDownloadPath(ani);
        if (!downloadPath.exists()) {
            return;
        }

        // tvshow.nfo
        String tvShowNfoFile = downloadPath.getParent() + "/tvshow.nfo";
        if (force || !FileUtil.exist(tvShowNfoFile)) {
            NfoGenerator.generateTvShowNfo(tmdb, tvShowNfoFile);
        }

        String posterPath = tmdb.getPosterPath();
        String fanartPath = tmdb.getBackdropPath();

        String posterExtName = FileUtil.extName(posterPath);
        String fanartExtName = FileUtil.extName(fanartPath);

        File posterFile = new File(downloadPath.getParent() + "/poster." + posterExtName);
        File fanartFile = new File(downloadPath.getParent() + "/fanart." + fanartExtName);

        // 封面、背景图
        saveImages(posterPath, posterFile, force);
        saveImages(fanartPath, fanartFile, force);

        // 保存logo
        TmdbImages tmdbImages = TmdbUtil.getTmdbImages(tmdb, TmdbTypeEnum.TV);
        List<TmdbImage> logos = tmdbImages.getLogos();
        if (!logos.isEmpty()) {
            TmdbImage tmdbImage = logos.get(0);
            String logoPath = tmdbImage.getFilePath();
            String extName = FileUtil.extName(logoPath);
            File logoFile = new File(downloadPath.getParent() + "/clearlogo." + extName);
            saveImages(logoPath, logoFile, force);
        }

        Integer season = ani.getSeason();

        Optional<TmdbSeason> optional = TmdbUtil.getTmdbSeason(tmdb, season);
        if (optional.isEmpty()) {
            return;
        }

        String seasonFormat = String.format("%02d", season);

        TmdbSeason tmdbSeason = optional.get();

        // 季封面
        String seasonPosterPath = tmdbSeason.getPosterPath();
        seasonPosterPath = StrUtil.blankToDefault(seasonPosterPath, posterPath);
        String seasonPosterExtName = FileUtil.extName(seasonPosterPath);
        File seasonPosterFile = new File(downloadPath.getParent() + "/season" + seasonFormat + "-poster." + seasonPosterExtName);
        saveImages(seasonPosterPath, seasonPosterFile, force);

        // 季nfo
        String seasonNfoFile = downloadPath + "/season.nfo";
        if (force || !FileUtil.exist(seasonNfoFile)) {
            NfoGenerator.generateSeasonNfo(tmdbSeason, seasonNfoFile);
        }

        File[] files = downloadPath.listFiles();
        if (Objects.isNull(files)) {
            // 不存在集
            return;
        }

        Map<Integer, TmdbEpisode> episodeMap = tmdbSeason
                .getEpisodes()
                .stream()
                .collect(Collectors.toMap(TmdbEpisode::getEpisodeNumber, it -> it));

        // 以下开始保存集的 thumb、nfo
        for (File file : files) {
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }

            if (!BaseDownload.videoFormat.contains(extName)) {
                // 非视频文件
                continue;
            }

            String mainName = FileUtil.mainName(file);
            if (!ReUtil.contains(StringEnum.SEASON_REG, mainName)) {
                // 命名不标准
                continue;
            }

            int seasonNumber = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, mainName, 1));
            if (season != seasonNumber) {
                // 季对应不上 跳过
                continue;
            }

            Integer episodeNumber =
                    Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, mainName, 2));
            if (!episodeMap.containsKey(episodeNumber)) {
                // 找不到对应集
                continue;
            }

            TmdbEpisode tmdbEpisode = episodeMap.get(episodeNumber);

            // thumb
            String thumbPath = tmdbEpisode.getStillPath();
            if (StrUtil.isNotBlank(thumbPath)) {
                String thumbExtName = FileUtil.extName(thumbPath);
                File thumbFile = new File(downloadPath + "/" + mainName + "-thumb." + thumbExtName);
                saveImages(thumbPath, thumbFile, force);
            }

            // 集图片
            String episodeFile = downloadPath + "/" + mainName + ".nfo";
            if (force || !FileUtil.exist(episodeFile)) {
                NfoGenerator.generateEpisodeNfo(tmdbEpisode, episodeFile);
            }
        }
    }

    /**
     * 保存图片
     *
     * @param tmdbPath tmdb路径
     * @param saveFile 保存位置
     * @param force    强制
     * @throws Exception
     */
    public static void saveImages(String tmdbPath, File saveFile, Boolean force) throws Exception {
        if (StrUtil.isBlank(tmdbPath)) {
            return;
        }

        if (!force) {
            if (saveFile.exists()) {
                return;
            }
        }

        FileUtil.del(saveFile);

        HttpReq.get("https://image.tmdb.org/t/p/original" + tmdbPath)
                .then(res -> {
                    try (InputStream inputStream = res.bodyStream()) {
                        FileUtil.writeFromStream(inputStream, saveFile, true);
                    } catch (Exception ignored) {
                    }
                });

        log.info("已保存图片 {}", saveFile);
    }

}
