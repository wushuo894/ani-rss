package ani.rss.service;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.commons.PinyinUtils;
import ani.rss.entity.*;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TorrentsTags;
import ani.rss.util.other.*;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 下载的主要逻辑
 */
@Slf4j
public class DownloadService {
    private static final String lock = "lock";

    /**
     * 下载动漫
     *
     * @param ani
     */
    @Synchronized("lock")
    public static void downloadAni(Ani ani) {
        Config config = ConfigUtil.CONFIG;
        Boolean delete = config.getDelete();
        Boolean autoDisabled = config.getAutoDisabled();
        Integer downloadCount = config.getDownloadCount();
        Integer delayedDownload = config.getDelayedDownload();
        Boolean deleteStandbyRSSOnly = config.getDeleteStandbyRSSOnly();

        String title = ani.getTitle();
        Integer season = ani.getSeason();
        Boolean downloadNew = ani.getDownloadNew();
        List<Double> notDownload = ani.getNotDownload();

        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();

        int currentDownloadCount = 0;
        List<Item> items = ItemsUtil.getItems(ani);

        ItemsUtil.omit(ani, items);
        log.debug("{} 共 {} 个", title, items.size());

        long count = torrentsInfos
                .stream()
                .filter(it -> {
                    TorrentsInfo.State state = it.getState();
                    if (Objects.isNull(state)) {
                        return true;
                    }
                    // 未下载完成
                    return !List.of(
                            TorrentsInfo.State.queuedUP.name(),
                            TorrentsInfo.State.uploading.name(),
                            TorrentsInfo.State.stalledUP.name(),
                            TorrentsInfo.State.pausedUP.name(),
                            TorrentsInfo.State.stoppedUP.name()
                    ).contains(state.name());
                })
                .count();

        String savePath = getActualDownloadPath(ani);
        String finalSavePath = getDownloadPath(ani);

        ItemsUtil.procrastinating(ani, items);

        // 实时保存文件
        boolean sync = false;

        for (Item item : items) {
            log.debug(JSONUtil.formatJsonStr(GsonStatic.toJson(item)));
            String reName = item.getReName();
            File torrent = TorrentUtil.getTorrent(ani, item);
            Boolean master = item.getMaster();
            String hash = FileUtil.mainName(torrent)
                    .trim().toLowerCase();

            Double episode = item.getEpisode();
            // .5 集
            boolean is5 = ItemsUtil.is5(episode);

            // 已经下载过
            if (torrent.exists()) {
                log.debug("种子记录已存在 {}", reName);
                if (master && !is5) {
                    currentDownloadCount++;
                }
                continue;
            }

            if (notDownload.contains(episode)) {
                if (master && !is5) {
                    currentDownloadCount++;
                }
                log.debug("已被禁止下载: {}", reName);
                continue;
            }

            // 只下载最新集
            if (downloadNew) {
                Item newItem = items.get(items.size() - 1);

                // 日期一致也可下载, 防止字幕组同时发多集
                Date pubDate = item.getPubDate();
                Date newPubDate = newItem.getPubDate();
                if (Objects.nonNull(pubDate) && Objects.nonNull(newPubDate)) {
                    String pubDateFormat = DateUtil.format(pubDate, "yyyy-MM-dd");
                    String newPubDateFormat = DateUtil.format(newPubDate, "yyyy-MM-dd");
                    // 日期不一致则跳过
                    if (!pubDateFormat.equals(newPubDateFormat)) {
                        if (master && !is5) {
                            currentDownloadCount++;
                        }
                        continue;
                    }
                } else if (item != newItem) {
                    if (master && !is5) {
                        currentDownloadCount++;
                    }
                    continue;
                }
            }

            Date pubDate = item.getPubDate();
            if (Objects.nonNull(pubDate) && delayedDownload > 0) {
                Date now = DateUtil.offset(new Date(), DateField.MINUTE, -delayedDownload);
                if (now.getTime() < pubDate.getTime()) {
                    log.info("延迟下载 {}", reName);
                    continue;
                }
            }

            // 仅在主RSS更新后删除备用RSS
            if (delete && master && deleteStandbyRSSOnly) {
                TorrentsInfo standbyRSS = torrentsInfos
                        .stream()
                        .filter(torrentsInfo -> {
                            String downloadDir = torrentsInfo.getDownloadDir();
                            if (!downloadDir.equals(savePath) && !downloadDir.equals(finalSavePath)) {
                                return false;
                            }
                            if (!ReUtil.contains(StringEnum.SEASON_REG, torrentsInfo.getName())) {
                                return false;
                            }
                            String s = ReUtil.get(StringEnum.SEASON_REG, torrentsInfo.getName(), 0);
                            if (!s.equals(ReUtil.get(StringEnum.SEASON_REG, reName, 0))) {
                                return false;
                            }
                            List<String> tags = torrentsInfo.getTags();
                            // 包含 备用RSS 标签或者 无主RSS字幕组信息
                            return tags.contains(TorrentsTags.BACK_RSS.getValue()) ||
                                    !tags.contains(ani.getSubgroup());
                        })
                        .findFirst()
                        .orElse(null);

                if (Objects.nonNull(standbyRSS)) {
                    List<String> tags = standbyRSS.getTags();
                    if (!tags.contains(TorrentsTags.RENAME.getValue())) {
                        // 未完成重命名
                        continue;
                    }
                    if (!TorrentUtil.delete(standbyRSS)) {
                        log.debug("备用RSS可能还未做种完成 {}", standbyRSS.getName());
                        // 删除失败或者不允许删除
                        continue;
                    }
                    torrentsInfos.remove(standbyRSS);
                }
            }

            // 已经下载过
            if (torrentsInfos
                    .stream()
                    .anyMatch(torrentsInfo ->
                            // hash 相同
                            torrentsInfo.getHash().equals(hash))) {
                log.info("已有下载任务 hash:{} name:{}", hash, reName);
                if (master && !is5) {
                    currentDownloadCount++;
                }
                continue;
            }

            // 未开启rename不进行检测
            if (itemDownloaded(ani, item, true)) {
                log.info("本地文件已存在 {}", reName);
                if (master && !is5) {
                    currentDownloadCount++;
                }
                continue;
            }

            // 同时下载数量限制
            if (downloadCount > 0) {
                if (count >= downloadCount) {
                    log.debug("达到同时下载数量限制 {}", downloadCount);
                    continue;
                }
            }

            File saveTorrent = TorrentUtil.saveTorrent(ani, item);

            if (!saveTorrent.exists()) {
                // 种子下载失败
                continue;
            }

            deleteStandbyRss(ani, item);

            if (!AniUtil.ANI_LIST.contains(ani)) {
                return;
            }

            sync = true;

            download(ani, item, savePath, saveTorrent);

            if (master && !is5) {
                currentDownloadCount++;
            }
            count++;
        }

        if (sync) {
            int size = ItemsUtil.currentEpisodeNumber(ani, items);
            // 更新当前集数
            ani.setCurrentEpisodeNumber(size);
            // 更新下载时间
            ani.setLastDownloadTime(System.currentTimeMillis());
            AniUtil.sync();
        }

        if (!autoDisabled) {
            return;
        }
        Integer totalEpisodeNumber = ani.getTotalEpisodeNumber();
        if (totalEpisodeNumber < 1) {
            return;
        }
        if (currentDownloadCount >= totalEpisodeNumber) {
            log.info("{} 第 {} 季 共 {} 集 已全部下载完成, 自动停止订阅", title, season, totalEpisodeNumber);
            NotificationUtil.send(config, ani, StrFormatter.format("{} 订阅已完结", title), NotificationStatusEnum.COMPLETED);
            ani.setEnable(false);
            AniUtil.sync();
        }
    }

    /**
     * 删除备用rss
     *
     * @param ani
     * @param item
     */
    public static void deleteStandbyRss(Ani ani, Item item) {
        Config config = ConfigUtil.CONFIG;
        Boolean standbyRss = config.getStandbyRss();
        Boolean coexist = config.getCoexist();
        Boolean delete = config.getDelete();
        String reName = item.getReName();

        if (!delete) {
            return;
        }

        if (!standbyRss) {
            return;
        }

        if (coexist) {
            // 开启多字幕组共存将不会进行洗版
            return;
        }

        if (!ReUtil.contains(StringEnum.SEASON_REG, reName)) {
            return;
        }
        reName = ReUtil.get(StringEnum.SEASON_REG, reName, 0);

        String downloadPath = getDownloadPath(ani);
        String actualDownloadPath = getActualDownloadPath(ani);

        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();

        String finalReName = reName;
        TorrentsInfo standbyRSS = torrentsInfos
                .stream()
                .filter(torrentsInfo -> {
                    String downloadDir = torrentsInfo.getDownloadDir();
                    if (!downloadDir.equals(downloadPath) && !downloadDir.equals(actualDownloadPath)) {
                        return false;
                    }
                    if (!ReUtil.contains(StringEnum.SEASON_REG, torrentsInfo.getName())) {
                        return false;
                    }
                    String s = ReUtil.get(StringEnum.SEASON_REG, torrentsInfo.getName(), 0);
                    return s.equals(finalReName);
                })
                .findFirst()
                .orElse(null);
        if (Objects.nonNull(standbyRSS)) {
            TorrentUtil.delete(standbyRSS, true, true);
        }

        File[] files = FileUtils.listFiles(downloadPath);
        for (File file : files) {
            String fileMainName = FileUtil.mainName(file);
            if (StrUtil.isBlank(fileMainName)) {
                continue;
            }
            if (!ReUtil.contains(StringEnum.SEASON_REG, fileMainName)) {
                continue;
            }
            fileMainName = ReUtil.get(StringEnum.SEASON_REG, fileMainName, 0);
            if (!fileMainName.equals(reName)) {
                continue;
            }
            boolean isDel = false;
            // 文件在删除前先判断其格式
            if (file.isFile()) {
                String extName = FileUtil.extName(file);
                // 没有后缀 跳过
                if (StrUtil.isBlank(extName)) {
                    continue;
                }
                if (FileUtils.isVideoFormat(extName)) {
                    isDel = true;
                }
                if (List.of("nfo", "bif").contains(extName)) {
                    isDel = true;
                }
                if (file.getName().endsWith("-thumb.jpg")) {
                    isDel = true;
                }
            }
            if (file.isDirectory()) {
                isDel = true;
            }
            if (isDel) {
                log.info("已开启备用RSS, 自动删除 {}", FileUtils.getAbsolutePath(file));
                try {
                    FileUtil.del(file);
                    log.info("删除成功 {}", FileUtils.getAbsolutePath(file));
                } catch (Exception e) {
                    log.error("删除失败 {}", FileUtils.getAbsolutePath(file));
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 下载
     *
     * @param ani
     * @param item
     * @param savePath
     * @param torrentFile
     */
    public static synchronized void download(Ani ani, Item item, String savePath, File torrentFile) {
        ani = ObjectUtil.clone(ani);

        String name = item.getReName();
        Boolean ova = ani.getOva();
        Boolean master = item.getMaster();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
        ani.setSubgroup(subgroup);

        log.info("添加下载 {}", name);

        if (!torrentFile.exists()) {
            log.error("种子下载出现问题 {} {}", name, FileUtils.getAbsolutePath(torrentFile));
            return;
        }
        ThreadUtil.sleep(1000);
        savePath = FileUtils.getAbsolutePath(savePath);

        String text = StrFormatter.format("{} 已更新", name);
        if (!master) {
            text = StrFormatter.format("(备用RSS) {}", text);
        }
        NotificationUtil.send(ConfigUtil.CONFIG, ani, text, NotificationStatusEnum.DOWNLOAD_START);

        Config config = ConfigUtil.CONFIG;

        Integer downloadRetry = config.getDownloadRetry();
        for (int i = 1; i <= downloadRetry; i++) {
            try {
                if (TorrentUtil.DOWNLOAD.download(ani, item, savePath, torrentFile, ova)) {
                    return;
                }
            } catch (Exception e) {
                String message = ExceptionUtils.getMessage(e);
                log.error(message, e);
            }
            log.error("{} 下载失败将进行重试, 当前重试次数为{}次", name, i);
        }

        // 删除下载失败的种子, 下次轮询仍会重试
        FileUtil.del(torrentFile);

        log.error("{} 添加失败，疑似为坏种", name);
        NotificationUtil.send(ConfigUtil.CONFIG, ani,
                StrFormatter.format("{} 添加失败，疑似为坏种", name),
                NotificationStatusEnum.ERROR);
    }

    /**
     * 下载完成通知
     *
     * @param torrentsInfo
     */
    public static synchronized void notification(TorrentsInfo torrentsInfo) {
        TorrentsInfo.State state = torrentsInfo.getState();
        String name = torrentsInfo.getName();

        if (Objects.isNull(state)) {
            return;
        }
        if (!List.of(
                TorrentsInfo.State.queuedUP.name(),
                TorrentsInfo.State.uploading.name(),
                TorrentsInfo.State.stalledUP.name(),
                TorrentsInfo.State.pausedUP.name(),
                TorrentsInfo.State.stoppedUP.name()
        ).contains(state.name())) {
            return;
        }
        // 添加下载完成标签，防止重复通知
        List<String> tags = torrentsInfo.getTags();
        if (tags.contains(TorrentsTags.DOWNLOAD_COMPLETE.getValue())) {
            return;
        }
        Boolean b = TorrentUtil.addTags(torrentsInfo, TorrentsTags.DOWNLOAD_COMPLETE.getValue());
        if (!b) {
            return;
        }

        Optional<Ani> aniOpt = findAniByDownloadPath(torrentsInfo);

        if (aniOpt.isEmpty()) {
            log.debug("未能获取番剧对象: {}", torrentsInfo.getName());
            return;
        }

        Ani ani = aniOpt.get();

        // 根据标签反向判断出字幕组
        String subgroup = ani.getSubgroup();
        Set<String> collect = ani.getStandbyRssList()
                .stream()
                .map(StandbyRss::getLabel)
                .collect(Collectors.toSet());

        subgroup = tags
                .stream()
                .filter(collect::contains)
                .findFirst()
                .orElse(subgroup);
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
        ani.setSubgroup(subgroup);

        Config config = ConfigUtil.CONFIG;
        Boolean scrape = config.getScrape();
        if (scrape) {
            // 刮削
            ScrapeService.scrape(ani, false);
        }
        String text = StrFormatter.format("{} 下载完成", name);
        if (tags.contains(TorrentsTags.BACK_RSS.getValue())) {
            text = StrFormatter.format("(备用RSS) {}", text);
        }
        NotificationUtil.send(ConfigUtil.CONFIG, ani, text, NotificationStatusEnum.DOWNLOAD_END);

        String title = ani.getTitle();

        try {
            AniUtil.completed(ani);
        } catch (Exception e) {
            log.error("番剧完结迁移失败 {}", title);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取下载位置
     *
     * @param ani
     * @return
     */
    public static String getDownloadPath(Ani ani) {
        return getDownloadPath(ani, ConfigUtil.CONFIG);
    }

    /**
     * 获取下载位置
     *
     * @param ani
     * @return
     */
    public static String getDownloadPath(Ani ani, Config config) {
        Boolean customDownloadPath = ani.getCustomDownloadPath();
        String aniDownloadPath = ani.getDownloadPath();
        Boolean ova = ani.getOva();

        String downloadPathTemplate = config.getDownloadPathTemplate();
        String ovaDownloadPathTemplate = config.getOvaDownloadPathTemplate();
        if (ova && StrUtil.isNotBlank(ovaDownloadPathTemplate)) {
            // 剧场版位置
            downloadPathTemplate = ovaDownloadPathTemplate;
        }

        if (customDownloadPath && StrUtil.isNotBlank(aniDownloadPath)) {
            // 自定义下载位置
            downloadPathTemplate = StrUtil.split(aniDownloadPath, "\n", true, true)
                    .stream()
                    .map(FileUtils::getAbsolutePath)
                    .findFirst()
                    .orElse(downloadPathTemplate);
        }

        String title = ani.getTitle().trim();

        String pinyin = PinyinUtils.getPinyin(title);
        String letter = pinyin.substring(0, 1).toUpperCase();
        if (ReUtil.isMatch("^\\d$", letter)) {
            letter = "0";
        } else if (!ReUtil.isMatch("^[a-zA-Z]$", letter)) {
            letter = "#";
        }

        downloadPathTemplate = downloadPathTemplate.replace("${letter}", letter);

        int year = ani.getYear();
        int month = ani.getMonth();
        String monthFormat = String.format("%02d", month);

        // 季度
        if (
                downloadPathTemplate.contains("${quarter}") ||
                        downloadPathTemplate.contains("${quarterFormat}") ||
                        downloadPathTemplate.contains("${quarterName}")
        ) {
            int quarter;
            String quarterName;
            /*
            https://github.com/wushuo894/ani-rss/pull/451
            优化季度判断规则，避免将月底先行播放的番归类到上个季度
            */
            if (List.of(12, 1, 2).contains(month)) {
                if (month == 12) {
                    // 当使用季度信息, 并且月份等于12时, 年份自动 +1。避免年份与月份不一致
                    year++;
                }
                quarter = 1;
                quarterName = "冬";
            } else if (List.of(3, 4, 5).contains(month)) {
                quarter = 4;
                quarterName = "春";
            } else if (List.of(6, 7, 8).contains(month)) {
                quarter = 7;
                quarterName = "夏";
            } else {
                quarter = 10;
                quarterName = "秋";
            }
            String quarterFormat = String.format("%02d", quarter);
            downloadPathTemplate = downloadPathTemplate.replace("${quarter}", String.valueOf(quarter));
            downloadPathTemplate = downloadPathTemplate.replace("${quarterFormat}", quarterFormat);
            downloadPathTemplate = downloadPathTemplate.replace("${quarterName}", quarterName);
        }

        downloadPathTemplate = downloadPathTemplate.replace("${year}", String.valueOf(year));
        downloadPathTemplate = downloadPathTemplate.replace("${month}", String.valueOf(month));
        downloadPathTemplate = downloadPathTemplate.replace("${monthFormat}", monthFormat);

        int season = ani.getSeason();
        String seasonFormat = String.format("%02d", season);

        downloadPathTemplate = downloadPathTemplate.replace("${season}", String.valueOf(season));
        downloadPathTemplate = downloadPathTemplate.replace("${seasonFormat}", seasonFormat);

        String bgmId = BgmUtil.getSubjectId(ani);
        downloadPathTemplate = downloadPathTemplate.replace("${bgmId}", bgmId);

        List<Func1<Ani, Object>> list = List.of(
                Ani::getTitle,
                Ani::getThemoviedbName,
                Ani::getSubgroup
        );

        downloadPathTemplate = RenameUtil.replaceField(downloadPathTemplate, ani, list);

        String tmdbId = Opt.ofNullable(ani.getTmdb())
                .map(Tmdb::getId)
                .filter(StrUtil::isNotBlank)
                .orElse("");

        downloadPathTemplate = downloadPathTemplate.replace("${tmdbid}", tmdbId);

        if (downloadPathTemplate.contains("${jpTitle}")) {
            String jpTitle = RenameUtil.getJpTitle(ani);
            downloadPathTemplate = downloadPathTemplate.replace("${jpTitle}", jpTitle);
        }

        return FileUtils.getAbsolutePath(downloadPathTemplate);
    }

    /**
     * 获取 BT 客户端实际下载位置
     * 如果 FFmpeg 转码开启，返回转码目标目录下的对应路径
     *
     * @param ani
     * @return
     */
    public static String getActualDownloadPath(Ani ani) {
        return getActualDownloadPath(ani, ConfigUtil.CONFIG);
    }

    /**
     * 获取 BT 客户端实际下载位置
     * 如果 FFmpeg 转码开启，返回转码目标目录下的对应路径
     *
     * @param ani
     * @param config
     * @return
     */
    public static String getActualDownloadPath(Ani ani, Config config) {
        String originalPath = getDownloadPath(ani, config);
        if (BooleanUtil.isTrue(config.getFfmpegEnable())) {
            String outputPath = config.getFfmpegOutputPath();
            if (StrUtil.isNotBlank(outputPath)) {
                String subPath = getRelativeSubPath(outputPath, originalPath);
                return FileUtils.getAbsolutePath(outputPath + File.separator + subPath);
            }
        }
        return originalPath;
    }

    /**
     * 计算 originalPath 相对于 transcodePath 的去重子路径
     * 去除两者的公共路径前缀，保留 originalPath 独有的部分
     * <p>
     * 例：transcodePath=/Media/transcode, originalPath=/Media/517057 → 517057
     * 例：transcodePath=/Media/transcode, originalPath=/Media/番剧/Title/Season 3 → 番剧/Title/Season 3
     */
    private static String getRelativeSubPath(String cachePath, String originalPath) {
        String[] cacheParts = cachePath.replace('\\', '/').split("/");
        String[] originalParts = originalPath.replace('\\', '/').split("/");

        int commonLen = 0;
        int minLen = Math.min(cacheParts.length, originalParts.length);
        for (int i = 0; i < minLen; i++) {
            if (cacheParts[i].equalsIgnoreCase(originalParts[i])) {
                commonLen = i + 1;
            } else {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = commonLen; i < originalParts.length; i++) {
            if (StrUtil.isNotBlank(originalParts[i])) {
                if (sb.length() > 0) {
                    sb.append(File.separator);
                }
                sb.append(originalParts[i]);
            }
        }

        String subPath = sb.toString();
        if (StrUtil.isBlank(subPath)) {
            subPath = new File(originalPath).getName();
        }
        return subPath;
    }


    /**
     * 判断是否已经下载过
     *
     * @param ani
     * @param item
     * @param downloadList
     * @return
     */
    public static Boolean itemDownloaded(Ani ani, Item item, Boolean downloadList) {
        Config config = ConfigUtil.CONFIG;
        Boolean rename = config.getRename();
        if (!rename) {
            return false;
        }

        String downloadPathTemplate = config.getDownloadPathTemplate();

        if (StrUtil.isBlank(downloadPathTemplate)) {
            return false;
        }

        Boolean fileExist = config.getFileExist();
        if (!fileExist) {
            return false;
        }

        Integer season = ani.getSeason();
        Boolean ova = ani.getOva();
        String reName = item.getReName();
        Double episode = item.getEpisode();

        String downloadPath = getDownloadPath(ani);
        String actualDownloadPath = getActualDownloadPath(ani);

        if (downloadList) {
            List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                String name = torrentsInfo.getName();
                if (!name.equalsIgnoreCase(reName)) {
                    continue;
                }
                String downloadDir = torrentsInfo.getDownloadDir();
                if (!downloadDir.equals(downloadPath) && !downloadDir.equals(actualDownloadPath)) {
                    continue;
                }
                log.info("已存在下载任务 {}", reName);
                TorrentUtil.saveTorrent(ani, item);
                return true;
            }
        }

        List<File> files = FileUtils.listFileList(downloadPath);

        if (files.stream()
                .filter(file -> {
                    if (file.isFile()) {
                        String extName = FileUtil.extName(file);
                        if (StrUtil.isBlank(extName)) {
                            return false;
                        }
                        return FileUtils.isVideoFormat(extName);
                    }
                    return true;
                })
                .anyMatch(file -> {
                    if (ova) {
                        return true;
                    }

                    String mainName = FileUtil.mainName(file);
                    if (StrUtil.isBlank(mainName)) {
                        return false;
                    }
                    mainName = mainName.trim().toUpperCase();
                    if (!ReUtil.contains(StringEnum.SEASON_REG, mainName)) {
                        return false;
                    }

                    String seasonStr = ReUtil.get(StringEnum.SEASON_REG, mainName, 1);

                    String episodeStr = ReUtil.get(StringEnum.SEASON_REG, mainName, 2);

                    if (StrUtil.isBlank(seasonStr) || StrUtil.isBlank(episodeStr)) {
                        return false;
                    }
                    return season == Integer.parseInt(seasonStr) && episode == Double.parseDouble(episodeStr);
                })) {
            // 保存 torrent 下次只校验 torrent 是否存在 ， 可以将config设置到固态硬盘，防止一直唤醒机械硬盘
            TorrentUtil.saveTorrent(ani, item);
            log.info("本地已存在 {}", reName);
            return true;
        }
        return false;
    }

    /**
     * 根据任务反查订阅
     *
     * @param torrentsInfo
     * @return
     */
    public static synchronized Optional<Ani> findAniByDownloadPath(TorrentsInfo torrentsInfo) {
        String downloadDir = FileUtils.normalize(torrentsInfo.getDownloadDir());
        return AniUtil.ANI_LIST
                .stream()
                .filter(ani -> {
                    String path = getDownloadPath(ani);
                    if (path.equals(downloadDir)) {
                        return true;
                    }
                    String actualPath = getActualDownloadPath(ani);
                    return actualPath.equals(downloadDir);
                })
                .map(ObjectUtil::clone)
                .findFirst();
    }

}
