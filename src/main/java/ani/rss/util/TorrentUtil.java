package ani.rss.util;

import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.json.JSONUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TorrentUtil {
    @Setter
    private static BaseDownload baseDownload;

    /**
     * 下载动漫
     *
     * @param ani
     */
    public static synchronized void downloadAni(Ani ani) {
        Config config = ConfigUtil.CONFIG;
        Boolean autoDisabled = config.getAutoDisabled();
        Integer downloadCount = config.getDownloadCount();
        Integer delayedDownload = config.getDelayedDownload();

        String title = ani.getTitle();
        Integer season = ani.getSeason();
        Boolean downloadNew = ani.getDownloadNew();

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();

        Set<String> downloadNameList = torrentsInfos.stream()
                .map(TorrentsInfo::getName)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> hashList = torrentsInfos
                .stream().map(TorrentsInfo::getHash)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int currentDownloadCount = 0;
        List<Item> items = ItemsUtil.getItems(ani);

        ItemsUtil.omit(ani, items);
        log.debug("{} 共 {} 个", title, items.size());

        if (downloadNew && !items.isEmpty()) {
            log.debug("{} 已开启只下载最新集", title);
            items = List.of(items.get(items.size() - 1));
        }

        long count = torrentsInfos
                .stream()
                .filter(it -> {
                    TorrentsInfo.State state = it.getState();
                    if (Objects.isNull(state)) {
                        return true;
                    }
                    // 未下载完成
                    return !List.of(
                            TorrentsInfo.State.uploading.name(),
                            TorrentsInfo.State.stalledUP.name(),
                            TorrentsInfo.State.pausedUP.name(),
                            TorrentsInfo.State.stoppedUP.name()
                    ).contains(state.name());
                })
                .count();

        for (Item item : items) {
            log.debug(JSONUtil.formatJsonStr(GsonStatic.toJson(item)));
            String reName = item.getReName();
            File torrent = getTorrent(ani, item);
            Boolean master = item.getMaster();
            String hash = FileUtil.mainName(torrent)
                    .trim().toLowerCase();

            Date pubDate = item.getPubDate();
            if (Objects.nonNull(pubDate) && delayedDownload > 0) {
                Date now = DateUtil.offset(new Date(), DateField.MINUTE, -delayedDownload);
                if (now.getTime() < pubDate.getTime()) {
                    log.info("延迟下载 {}", reName);
                    continue;
                }
            }

            // 已经下载过
            if (hashList.contains(hash) || downloadNameList.contains(reName)) {
                log.debug("已有下载任务 {}", reName);
                if (master && !reName.endsWith(".5")) {
                    currentDownloadCount++;
                }
                continue;
            }

            // 已经下载过
            if (torrent.exists()) {
                log.debug("种子记录已存在 {}", reName);
                if (master && !reName.endsWith(".5")) {
                    currentDownloadCount++;
                }
                continue;
            }

            // 未开启rename不进行检测
            if (itemDownloaded(ani, item, true)) {
                log.debug("本地文件已存在 {}", reName);
                if (master && !reName.endsWith(".5")) {
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

            log.info("添加下载 {}", reName);
            File saveTorrent = saveTorrent(ani, item);
            List<File> downloadPathList = getDownloadPath(ani);

            deleteBackRss(ani, item);

            String savePath = downloadPathList
                    .get(0)
                    .toString();

            int size = ItemsUtil.currentEpisodeNumber(ani, items);
            if (size > 0 && ani.getCurrentEpisodeNumber() < size) {
                ani.setCurrentEpisodeNumber(size);
                AniUtil.sync();
            }

            download(ani, item, savePath, saveTorrent);
            if (master && !reName.endsWith(".5")) {
                currentDownloadCount++;
            }
            count++;
        }

        int size = ItemsUtil.currentEpisodeNumber(ani, items);
        if (size > 0 && ani.getCurrentEpisodeNumber() != size) {
            ani.setCurrentEpisodeNumber(size);
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
            ani.setEnable(false);
            log.info("{} 第 {} 季 共 {} 集 已全部下载完成, 自动停止订阅", title, season, totalEpisodeNumber);
            AniUtil.sync();
        }
    }

    /**
     * 删除备用rss
     *
     * @param ani
     * @param item
     */
    public static void deleteBackRss(Ani ani, Item item) {
        Config config = ConfigUtil.CONFIG;
        Boolean backRss = config.getBackRss();
        Boolean delete = config.getDelete();
        String reName = item.getReName();

        if (!delete) {
            return;
        }

        if (!backRss) {
            return;
        }
        List<Ani.BackRss> backRssList = ani.getBackRssList();
        if (backRssList.isEmpty()) {
            return;
        }
        if (!ReUtil.contains(StringEnum.SEASON_REG, reName)) {
            return;
        }
        reName = ReUtil.get(StringEnum.SEASON_REG, reName, 0);

        List<File> downloadPathList = getDownloadPath(ani);

        List<File> files = downloadPathList.stream()
                .filter(File::exists)
                .filter(File::isDirectory)
                .flatMap(downloadPath -> Stream.of(ObjectUtil.defaultIfNull(downloadPath.listFiles(), new File[]{})))
                .collect(Collectors.toList());
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
                for (String en : BaseDownload.videoFormat) {
                    // 后缀匹配不上 跳过
                    if (!extName.equalsIgnoreCase(en)) {
                        continue;
                    }
                    isDel = true;
                    break;
                }
            }
            if (file.isDirectory()) {
                isDel = true;
            }
            if (isDel) {
                log.info("已开启备用RSS, 自动删除 {}", file.getAbsolutePath());
                try {
                    FileUtil.del(file);
                    log.info("删除成功 {}", file.getAbsolutePath());
                } catch (Exception e) {
                    log.error("删除失败 {}", file.getAbsolutePath());
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static File getTorrentDir(Ani ani) {
        String title = ani.getTitle();
        Boolean ova = ani.getOva();
        Integer season = ani.getSeason();

        File configDir = ConfigUtil.getConfigDir();

        String pinyin = PinyinUtil.getPinyin(title);
        String s = pinyin.toUpperCase().substring(0, 1);
        if (ReUtil.isMatch("^\\d$", s)) {
            s = "0";
        } else if (!ReUtil.isMatch("^[a-zA-Z]$", s)) {
            s = "#";
        }

        File torrents = new File(StrFormatter.format("{}/torrents/{}/Season {}", configDir, title, season));
        if (!torrents.exists()) {
            torrents = new File(StrFormatter.format("{}/torrents/{}/{}/Season {}", configDir, s, title, season));
        }
        if (ova) {
            torrents = new File(StrFormatter.format("{}/torrents/{}", configDir, title));
            if (!torrents.exists()) {
                torrents = new File(StrFormatter.format("{}/torrents/{}/{}", configDir, s, title));
            }
        }
        FileUtil.mkdir(torrents);
        return torrents;
    }

    public static File getTorrent(Ani ani, Item item) {
        String infoHash = item.getInfoHash();
        File torrents = getTorrentDir(ani);
        String torrent = item.getTorrent();
        if (ReUtil.contains(StringEnum.MAGNET_REG, torrent)) {
            return new File(torrents + "/" + infoHash + ".txt");
        }
        return new File(torrents + "/" + infoHash + ".torrent");
    }

    /**
     * 下载种子文件
     *
     * @param item
     */
    public static File saveTorrent(Ani ani, Item item) {
        String torrent = item.getTorrent();
        String reName = item.getReName();

        log.info("下载种子 {}", reName);
        File saveTorrentFile = getTorrent(ani, item);
        if (saveTorrentFile.exists()) {
            return saveTorrentFile;
        }

        try {
            if (ReUtil.contains(StringEnum.MAGNET_REG, torrent)) {
                FileUtil.writeUtf8String(torrent, saveTorrentFile);
                return saveTorrentFile;
            }

            return HttpReq.get(torrent, true)
                    .thenFunction(res -> {
                        FileUtil.writeFromStream(res.bodyStream(), saveTorrentFile, true);
                        return saveTorrentFile;
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error("下载种子时出现问题 {}", message);
            log.error(message, e);
        }
        return saveTorrentFile;
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

        String downloadPath = config.getDownloadPath();

        if (StrUtil.isBlank(downloadPath)) {
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

        if (downloadList) {
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                String name = torrentsInfo.getName();
                if (name.equalsIgnoreCase(reName)) {
                    log.info("已存在下载任务 {}", reName);
                    saveTorrent(ani, item);
                    return true;
                }
            }
        }

        List<File> files = getDownloadPath(ani)
                .stream()
                .flatMap(file -> {
                    if (ova) {
                        return FileUtil.loopFiles(file).stream();
                    }
                    return Stream.of(ObjectUtil.defaultIfNull(file.listFiles(), new File[]{}));
                })
                .collect(Collectors.toList());

        if (files.stream()
                .filter(file -> {
                    if (file.isFile()) {
                        String extName = FileUtil.extName(file);
                        if (StrUtil.isBlank(extName)) {
                            return false;
                        }
                        return BaseDownload.videoFormat.contains(extName);
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
            saveTorrent(ani, item);
            log.info("本地已存在 {}", reName);
            return true;
        }

        return false;
    }

    /**
     * 获取下载位置
     *
     * @param ani
     * @return
     */
    public static List<File> getDownloadPath(Ani ani) {
        Boolean customDownloadPath = ani.getCustomDownloadPath();
        String aniDownloadPath = ani.getDownloadPath();

        if (customDownloadPath && StrUtil.isNotBlank(aniDownloadPath)) {
            List<File> files = StrUtil.split(aniDownloadPath, "\n", true, true)
                    .stream()
                    .map(File::new)
                    .collect(Collectors.toList());
            if (!files.isEmpty()) {
                return files;
            }
        }

        String title = ani.getTitle().trim();
        Integer season = ani.getSeason();
        Boolean ova = ani.getOva();

        Config config = ConfigUtil.CONFIG;
        String downloadPath = config.getDownloadPath();
        String ovaDownloadPath = config.getOvaDownloadPath();
        // 按拼音首字母存放
        Boolean acronym = config.getAcronym();
        // 根据季度存放
        Boolean quarter = config.getQuarter();
        Boolean fileExist = config.getFileExist();
        if (ova && StrUtil.isNotBlank(ovaDownloadPath)) {
            downloadPath = ovaDownloadPath;
        }
        if (acronym) {
            String pinyin = PinyinUtil.getPinyin(title);
            String s = pinyin.substring(0, 1).toUpperCase();
            if (ReUtil.isMatch("^\\d$", s)) {
                s = "0";
            } else if (!ReUtil.isMatch("^[a-zA-Z]$", s)) {
                s = "#";
            }
            downloadPath += "/" + s;
        } else if (quarter) {
            Integer year = ani.getYear();
            Integer month = ani.getMonth();
            downloadPath = StrFormatter.format("{}/{}-{}", downloadPath, year, String.format("%02d", month));
        }
        if (ova) {
            return List.of(new File(downloadPath + "/" + title));
        }

        String seasonFileName = "";
        String seasonName = config.getSeasonName();
        if ("Season 1".equals(seasonName)) {
            seasonFileName = StrFormatter.format("Season {}", season);
        }
        if ("Season 01".equals(seasonName)) {
            seasonFileName = StrFormatter.format("Season {}", String.format("%02d", season));
        }
        if ("S1".equals(seasonName)) {
            seasonFileName = StrFormatter.format("S{}", season);
        }
        if ("S01".equals(seasonName)) {
            seasonFileName = StrFormatter.format("S{}", String.format("%02d", season));
        }

        File file = new File(StrFormatter.format("{}/{}/{}", downloadPath, title, seasonFileName));
        List<File> files = new ArrayList<>();
        if (!fileExist) {
            files.add(file);
            return files;
        }
        File aniFile = new File(downloadPath + "/" + title);
        if (!aniFile.exists()) {
            files.add(file);
            return files;
        }

        File[] seasonFiles = ObjectUtil.defaultIfNull(aniFile.listFiles(), new File[]{});
        for (File seasonFile : seasonFiles) {
            if (!seasonFile.isDirectory()) {
                continue;
            }
            String name = seasonFile.getName();
            String s1 = ReUtil.get("^[a-zA-Z]+", name, 0);
            if (StrUtil.isBlank(s1)) {
                continue;
            }
            if ((!s1.equalsIgnoreCase("S")) && (!s1.equalsIgnoreCase("Season"))) {
                continue;
            }
            String s = ReUtil.get("\\d+$", name, 0);
            if (StrUtil.isBlank(s)) {
                continue;
            }
            if (!NumberUtil.isNumber(s)) {
                continue;
            }
            Integer sInt = Integer.parseInt(s);
            if (!NumberUtil.equals(sInt, season)) {
                continue;
            }
            files.add(seasonFile);
        }
        files.add(file);
        return files;
    }

    /**
     * 登录 qBittorrent
     *
     * @return
     */
    public static synchronized Boolean login() {
        ThreadUtil.sleep(1000);
        Config config = ConfigUtil.CONFIG;
        String downloadPath = config.getDownloadPath();
        if (StrUtil.isBlank(downloadPath)) {
            log.warn("下载位置未设置");
            return false;
        }
        return baseDownload.login(ConfigUtil.CONFIG);
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
        String name = item.getReName();
        Boolean ova = ani.getOva();
        Boolean master = item.getMaster();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");

        ani = ObjectUtil.clone(ani).setSubgroup(subgroup);

        Config config = ConfigUtil.CONFIG;
        Boolean backRss = config.getBackRss();

        if (!torrentFile.exists()) {
            log.error("种子下载出现问题 {} {}", name, torrentFile.getAbsolutePath());
            MessageUtil.send(ConfigUtil.CONFIG, ani,
                    StrFormatter.format("种子下载出现问题 {} {}", name, torrentFile.getAbsolutePath()),
                    MessageEnum.ERROR
            );
            return;
        }
        ThreadUtil.sleep(1000);
        savePath = savePath.replace("\\", "/");

        String text = StrFormatter.format("{} 已更新", name);
        if (backRss && !ani.getBackRssList().isEmpty()) {
            text = StrFormatter.format("({}) {}", master ? "主RSS" : "备用RSS", text);
        }
        MessageUtil.send(ConfigUtil.CONFIG, ani, text, MessageEnum.DOWNLOAD_START);

        try {
            if (baseDownload.download(item, savePath, torrentFile, ova)) {
                return;
            }
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
        }
        log.error("{} 添加失败，疑似为坏种", name);
        MessageUtil.send(ConfigUtil.CONFIG, ani,
                StrFormatter.format("{} 添加失败，疑似为坏种", name),
                MessageEnum.ERROR);
    }

    /**
     * 获取任务列表
     *
     * @return
     */
    public static synchronized List<TorrentsInfo> getTorrentsInfos() {
        ThreadUtil.sleep(1000);
        return baseDownload.getTorrentsInfos();
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
                TorrentsInfo.State.uploading.name(),
                TorrentsInfo.State.stalledUP.name(),
                TorrentsInfo.State.pausedUP.name(),
                TorrentsInfo.State.stoppedUP.name()
        ).contains(state.name())) {
            return;
        }
        // 添加下载完成标签，防止重复通知
        String tags = torrentsInfo.getTags();
        List<String> tagList = StrUtil.split(tags, ",", true, true);
        if (tagList.contains("下载完成")) {
            return;
        }
        Boolean b = TorrentUtil.addTags(torrentsInfo, "下载完成");
        if (!b) {
            return;
        }
        Ani ani = null;
        try {
            ani = findAniByName(name);
            String subgroup = tagList
                    .stream()
                    .filter(s -> !BaseDownload.tag.equals(s))
                    .filter(s -> !"RENAME".equals(s))
                    .findFirst()
                    .orElse("");
            subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
            if (Objects.nonNull(ani)) {
                ani.setSubgroup(subgroup);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        MessageUtil.send(ConfigUtil.CONFIG, ani, name + " 下载完成", MessageEnum.DOWNLOAD_END);
    }

    /**
     * 根据通知反查订阅
     *
     * @param name
     * @return
     */
    public static synchronized Ani findAniByName(String name) {
        if (!ReUtil.contains(StringEnum.SEASON_REG, name)) {
            return AniUtil.ANI_LIST
                    .stream()
                    .filter(ani -> ani.getTitle().equals(name))
                    .findFirst()
                    .orElse(null);
        }
        Config config = ConfigUtil.CONFIG;
        String renameTemplate = config.getRenameTemplate();
        renameTemplate = renameTemplate
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]");

        String title;
        int season;

        int titleIndex = renameTemplate.indexOf("${title}");

        if (titleIndex < 0) {
            return null;
        }

        int seasonIndex = renameTemplate.indexOf("${seasonFormat}");

        if (seasonIndex < 1) {
            seasonIndex = renameTemplate.indexOf("${season}");
        }

        if (seasonIndex < 1) {
            return null;
        }

        renameTemplate = renameTemplate
                .replace("${title}", "(.+)")
                .replace("${seasonFormat}", "(\\d+)")
                .replace("${season}", "(\\d+)");

        renameTemplate = renameTemplate.replaceAll("\\$\\{\\w+}", ".+");

        title = ReUtil.get(renameTemplate, name, titleIndex < seasonIndex ? 1 : 2);
        season = Integer.parseInt(ReUtil.get(renameTemplate, name, titleIndex < seasonIndex ? 2 : 1));

        title = title.replaceAll(StringEnum.YEAR_REG, "");
        title = title.replaceAll(StringEnum.TMDB_ID_REG, "");
        title = title.trim();

        for (Ani ani : AniUtil.ANI_LIST) {
            String aniTitle = ani.getTitle();
            aniTitle = aniTitle.replaceAll(StringEnum.YEAR_REG, "");
            aniTitle = aniTitle.replaceAll(StringEnum.TMDB_ID_REG, "");
            aniTitle = aniTitle.trim();
            if (!title.equals(aniTitle)) {
                continue;
            }
            if (season != ani.getSeason()) {
                continue;
            }
            return ObjectUtil.clone(ani);
        }

        return null;
    }

    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    public static synchronized void delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean delete = config.getDelete();
        Boolean awaitStalledUP = config.getAwaitStalledUP();

        TorrentsInfo.State state = torrentsInfo.getState();
        String name = torrentsInfo.getName();

        if (Objects.isNull(state)) {
            return;
        }

        // 是否等待做种完毕
        if (awaitStalledUP) {
            if (!List.of(
                    TorrentsInfo.State.pausedUP.name(),
                    TorrentsInfo.State.stoppedUP.name()
            ).contains(state.name())) {
                return;
            }
        } else {
            if (!List.of(
                    TorrentsInfo.State.uploading.name(),
                    TorrentsInfo.State.stalledUP.name(),
                    TorrentsInfo.State.pausedUP.name(),
                    TorrentsInfo.State.stoppedUP.name()
            ).contains(state.name())) {
                return;
            }
        }


        if (delete) {
            log.info("删除已完成任务 {}", name);
            ThreadUtil.sleep(1000);
            baseDownload.delete(torrentsInfo);
        }
    }

    /**
     * 重命名
     *
     * @param torrentsInfo
     */
    public static synchronized void rename(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean rename = config.getRename();
        if (!rename) {
            return;
        }

        String tags = torrentsInfo.getTags();
        if (StrUtil.split(tags, ",", true, true)
                .contains("RENAME")) {
            return;
        }

        ThreadUtil.sleep(1000);
        baseDownload.rename(torrentsInfo);
        addTags(torrentsInfo, "RENAME");
    }

    public static Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        if (StrUtil.isBlank(tags)) {
            return false;
        }
        try {
            return baseDownload.addTags(torrentsInfo, tags);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public static synchronized void load() {
        Config config = ConfigUtil.CONFIG;
        String download = config.getDownload();
        ClassUtil.scanPackage("ani.rss.download")
                .stream()
                .filter(aClass -> !aClass.isInterface())
                .filter(aClass -> aClass.getSimpleName().equals(download))
                .map(aClass -> (BaseDownload) ReflectUtil.newInstance(aClass))
                .findFirst()
                .ifPresent(TorrentUtil::setBaseDownload);
        BaseDownload.renameCache.clear();
        log.info("下载工具 {}", download);
    }

}
