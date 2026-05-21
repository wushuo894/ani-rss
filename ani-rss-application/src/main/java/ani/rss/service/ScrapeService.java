package ani.rss.service;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.BgmUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.TmdbUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import wushuo.tmdb.api.entity.*;
import wushuo.tmdb.api.enums.TmdbTypeEnum;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 刮削
 */
@Slf4j
@Service
public class ScrapeService {

    @Resource
    private NfoGenerator nfoGenerator;

    @Lazy
    @Resource
    private DownloadService downloadService;

    /**
     * 刮削
     *
     * @param ani         订阅
     * @param forceScrape 强制刮削
     */
    public void scrape(Ani ani, Boolean forceScrape) {
        String title = ani.getTitle();

        Tmdb tmdb = ani.getTmdb();

        if (Objects.isNull(tmdb)) {
            return;
        }

        boolean isOva = ani.getOva();
        try {
            log.info("正在刮削 ... {}", title);
            saveBangumiIni(ani, forceScrape);
            if (isOva) {
                scrapeMovie(ani, forceScrape);
            } else {
                scrapeTv(ani, forceScrape);
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
    public void scrapeMovie(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        // 更新tmdb信息
        Optional<Tmdb> tmdbOptional = TmdbUtils.getTmdb(tmdb, TmdbTypeEnum.MOVIE);
        if (tmdbOptional.isEmpty()) {
            log.warn("获取tmdb失败 {}", tmdb.getId());
            return;
        }
        tmdb = tmdbOptional.get();

        // 下载位置
        String downloadPath = downloadService.getDownloadPath(ani);
        File[] files = FileUtils.listFiles(downloadPath);

        if (ArrayUtil.isEmpty(files)) {
            return;
        }

        Optional<File> first = Stream.of(files)
                .filter(file -> {
                    String extName = FileUtil.extName(file);
                    if (StrUtil.isBlank(extName)) {
                        return false;
                    }
                    return FileUtils.isVideoFormat(extName);
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
            nfoGenerator.generateMovieNfo(tmdb, outputPath);
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

        TmdbImages tmdbImages = TmdbUtils.getTmdbImages(tmdb, TmdbTypeEnum.MOVIE);
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
    public void scrapeTv(Ani ani, Boolean force) throws Exception {
        Tmdb tmdb = ani.getTmdb();

        // 更新tmdb信息
        Optional<Tmdb> tmdbOptional = TmdbUtils.getTmdb(tmdb, TmdbTypeEnum.TV);
        if (tmdbOptional.isEmpty()) {
            log.warn("获取tmdb失败 {}", tmdb.getId());
            return;
        }
        tmdb = tmdbOptional.get();

        // 下载位置
        File downloadPath = new File(downloadService.getDownloadPath(ani));
        if (!FileUtil.exist(downloadPath)) {
            return;
        }

        // tvshow.nfo
        String tvShowNfoFile = downloadPath.getParent() + "/tvshow.nfo";
        if (force || !FileUtil.exist(tvShowNfoFile)) {
            nfoGenerator.generateTvShowNfo(tmdb, tvShowNfoFile);
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
        TmdbImages tmdbImages = TmdbUtils.getTmdbImages(tmdb, TmdbTypeEnum.TV);
        List<TmdbImage> logos = tmdbImages.getLogos();
        if (!logos.isEmpty()) {
            TmdbImage tmdbImage = logos.get(0);
            String logoPath = tmdbImage.getFilePath();
            String extName = FileUtil.extName(logoPath);
            File logoFile = new File(downloadPath.getParent() + "/clearlogo." + extName);
            saveImages(logoPath, logoFile, force);
        }

        Integer season = ani.getSeason();

        Optional<TmdbSeason> optional = TmdbUtils.getTmdbSeason(tmdb, season);
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
            nfoGenerator.generateSeasonNfo(tmdbSeason, seasonNfoFile);
        }

        File[] files = FileUtils.listFiles(downloadPath);

        Map<Integer, TmdbEpisode> episodeMap = tmdbSeason
                .getEpisodes()
                .stream()
                .collect(Collectors.toMap(TmdbEpisode::getEpisodeNumber, it -> it));

        Config config = ConfigUtil.CONFIG;
        // 追更天数
        Integer followDay = config.getFollowDay();

        // 以下开始保存集的 thumb、nfo
        for (File file : files) {
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }

            if (!FileUtils.isVideoFormat(extName)) {
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

            // 该集的播出日期
            Date airDate = Optional.of(tmdbEpisode)
                    .map(TmdbEpisode::getAirDate)
                    .orElse(new Date());

            // 最晚追更时间
            Date date = DateUtil.offsetDay(new Date(), -followDay);

            // 播出日期 >= 最晚追更时间 强制刷新元数据
            boolean isFollow = airDate.getTime() >= date.getTime();

            // thumb
            String thumbPath = tmdbEpisode.getStillPath();
            if (StrUtil.isNotBlank(thumbPath)) {
                String thumbExtName = FileUtil.extName(thumbPath);
                File thumbFile = new File(downloadPath + "/" + mainName + "-thumb." + thumbExtName);

                // 判断条件: 追更 or 强制
                saveImages(thumbPath, thumbFile, isFollow || force);
            }

            // nfo
            String episodeFile = downloadPath + "/" + mainName + ".nfo";
            // 判断条件: 追更 or 强制 or 元数据不存在
            if (isFollow || force || !FileUtil.exist(episodeFile)) {
                nfoGenerator.generateEpisodeNfo(tmdbEpisode, episodeFile);
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
    public void saveImages(String tmdbPath, File saveFile, Boolean force) throws Exception {
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

    /**
     * 保存 bangumi.ini
     * @param ani 订阅
     * @param force 强制
     */
    public void saveBangumiIni(Ani ani, Boolean force) throws Exception {
        Config config = ConfigUtil.CONFIG;
        Boolean bangumiIniEnabled = config.getBangumiIniEnabled();
        if (!bangumiIniEnabled) {
            // 未开启 bangumi.ini
            return;
        }

        String downloadPath = downloadService.getDownloadPath(ani);

        File file = new File(downloadPath, "bangumi.ini");
        if (!force) {
            if (file.exists()) {
                // 非强制模式
                return;
            }
        }

        String subjectId = BgmUtil.getSubjectId(ani);
        Integer offset = ani.getOffset();

        String s = """
                [Bangumi]
                id={}
                offset={}
                """;

        s = StrUtil.format(s, subjectId, offset);

        FileUtil.writeUtf8String(s, file);

        log.info("已保存 {}", file);
    }

}
