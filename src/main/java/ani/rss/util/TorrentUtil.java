package ani.rss.util;

import ani.rss.action.ClearCacheAction;
import ani.rss.download.BaseDownload;
import ani.rss.entity.*;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TorrentsTags;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.json.JSONUtil;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.bittorrent.TorrentFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TorrentUtil {
    @Setter
    private static BaseDownload baseDownload;

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
        Boolean deleteBackRSSOnly = config.getDeleteBackRSSOnly();

        String title = ani.getTitle();
        Integer season = ani.getSeason();
        Boolean downloadNew = ani.getDownloadNew();
        List<Double> notDownload = ani.getNotDownload();

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();

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

        List<File> downloadPathList = getDownloadPath(ani);
        String savePath = FilePathUtil.getAbsolutePath(
                downloadPathList
                        .get(0)
        );

        ItemsUtil.procrastinating(ani, items);

        for (Item item : items) {
            log.debug(JSONUtil.formatJsonStr(GsonStatic.toJson(item)));
            String reName = item.getReName();
            File torrent = getTorrent(ani, item);
            Boolean master = item.getMaster();
            String hash = FileUtil.mainName(torrent)
                    .trim().toLowerCase();

            Double episode = item.getEpisode();
            // .5 集
            boolean is5 = episode.intValue() != episode;

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
            if (delete && master && deleteBackRSSOnly) {
                TorrentsInfo backRSS = torrentsInfos
                        .stream()
                        .filter(torrentsInfo -> {
                            if (!torrentsInfo.getDownloadDir().equals(savePath)) {
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
                            return tags.contains(TorrentsTags.BACK_RSS.getValue());
                        })
                        .findFirst()
                        .orElse(null);

                if (Objects.nonNull(backRSS)) {
                    List<String> tags = backRSS.getTags();
                    if (!tags.contains(TorrentsTags.RENAME.getValue())) {
                        // 未完成重命名
                        continue;
                    }
                    if (!delete(backRSS)) {
                        log.debug("备用RSS可能还未做种完成 {}", backRSS.getName());
                        // 删除失败或者不允许删除
                        continue;
                    }
                    torrentsInfos.remove(backRSS);
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

            File saveTorrent = saveTorrent(ani, item);

            if (!saveTorrent.exists()) {
                // 种子下载失败
                continue;
            }

            deleteBackRss(ani, item);

            if (!AniUtil.ANI_LIST.contains(ani)) {
                return;
            }
            download(ani, item, savePath, saveTorrent);
            if (master && !is5) {
                currentDownloadCount++;
            }
            count++;
        }

        int size = ItemsUtil.currentEpisodeNumber(ani, items);
        if (size > 0 && ani.getCurrentEpisodeNumber() != size) {
            // 当前集数发生变动
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
            MessageUtil.send(config, ani, StrFormatter.format("{} 订阅已完结", title), MessageEnum.COMPLETED);
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
        Boolean coexist = config.getCoexist();
        Boolean delete = config.getDelete();
        String reName = item.getReName();

        if (!delete) {
            return;
        }

        if (!backRss) {
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

        List<File> downloadPathList = getDownloadPath(ani);

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();

        for (File file : downloadPathList) {
            String finalReName = reName;
            TorrentsInfo backRSS = torrentsInfos
                    .stream()
                    .filter(torrentsInfo -> {
                        if (!torrentsInfo.getDownloadDir().equals(FilePathUtil.getAbsolutePath(file))) {
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
            if (Objects.nonNull(backRSS)) {
                TorrentUtil.delete(backRSS, true, true);
            }
        }

        List<File> files = downloadPathList.stream()
                .filter(File::exists)
                .filter(File::isDirectory)
                .flatMap(downloadPath -> Stream.of(ObjectUtil.defaultIfNull(downloadPath.listFiles(), new File[]{})))
                .toList();

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
                extName = extName.toLowerCase();
                if (BaseDownload.videoFormat.contains(extName)) {
                    isDel = true;
                }
                if (extName.equals("nfo")) {
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
                log.info("已开启备用RSS, 自动删除 {}", FilePathUtil.getAbsolutePath(file));
                try {
                    FileUtil.del(file);
                    log.info("删除成功 {}", FilePathUtil.getAbsolutePath(file));
                } catch (Exception e) {
                    log.error("删除失败 {}", FilePathUtil.getAbsolutePath(file));
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
                log.info("种子下载完成 {}", reName);
                return saveTorrentFile;
            }

            return HttpReq.get(torrent, true)
                    .thenFunction(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            // 如果为 404 则写入空文件 已在 getMagnet 处理过
                            FileUtil.writeUtf8String("", saveTorrentFile);
                            log.info("种子下载完成 {}", reName);
                            return saveTorrentFile;
                        }
                        HttpReq.assertStatus(res);
                        FileUtil.writeFromStream(res.bodyStream(), saveTorrentFile, true);
                        log.info("种子下载完成 {}", reName);
                        return saveTorrentFile;
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error("下载种子时出现问题 {}", message);
            log.error(message, e);
            // 种子未下载异常，删除
            FileUtil.del(saveTorrentFile);
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

        String downloadPath = FilePathUtil.getAbsolutePath(config.getDownloadPath());

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
                .toList();

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
                    .toList();
            if (!files.isEmpty()) {
                return files;
            }
        }

        String title = ani.getTitle().trim();
        Integer season = ani.getSeason();
        Boolean ova = ani.getOva();

        Config config = ConfigUtil.CONFIG;
        String downloadPath = FilePathUtil.getAbsolutePath(config.getDownloadPath());
        String ovaDownloadPath = FilePathUtil.getAbsolutePath(config.getOvaDownloadPath());
        // 按拼音首字母存放
        Boolean acronym = config.getAcronym();
        // 根据季度存放
        Boolean quarter = config.getQuarter();
        Boolean quarterMerge = config.getQuarterMerge();
        // 按照年份存放
        Boolean yearStorage = config.getYearStorage();
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
            if (quarterMerge) {
                if (List.of(1, 2, 3).contains(month)) {
                    month = 1;
                } else if (List.of(4, 5, 6).contains(month)) {
                    month = 4;
                } else if (List.of(7, 8, 9).contains(month)) {
                    month = 7;
                } else {
                    month = 10;
                }
            }
            downloadPath = StrFormatter.format("{}/{}-{}", downloadPath, year, String.format("%02d", month));
        } else if (yearStorage) {
            Integer year = ani.getYear();
            downloadPath = StrFormatter.format("{}/{}", downloadPath, year);
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
            if (seasonFile.isFile()) {
                continue;
            }
            String name = seasonFile.getName();
            String regStr = "([Ss]eason|[Ss]) ?(\\d+)";
            if (!ReUtil.contains(regStr, name)) {
                continue;
            }
            String s = ReUtil.get(regStr, name, 2);
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
        try {
            return baseDownload.login(ConfigUtil.CONFIG);
        } catch (Exception e) {
            return false;
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
            log.error("种子下载出现问题 {} {}", name, FilePathUtil.getAbsolutePath(torrentFile));
            return;
        }
        ThreadUtil.sleep(1000);
        savePath = FilePathUtil.getAbsolutePath(savePath);

        String text = StrFormatter.format("{} 已更新", name);
        if (!master) {
            text = StrFormatter.format("(备用RSS) {}", text);
        }
        MessageUtil.send(ConfigUtil.CONFIG, ani, text, MessageEnum.DOWNLOAD_START);

        try {
            createTvShowNfo(savePath, ani);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        Config config = ConfigUtil.CONFIG;

        Integer downloadRetry = config.getDownloadRetry();
        for (int i = 1; i <= downloadRetry; i++) {
            try {
                if (baseDownload.download(ani, item, savePath, torrentFile, ova)) {
                    return;
                }
            } catch (Exception e) {
                String message = ExceptionUtil.getMessage(e);
                log.error(message, e);
            }
            log.error("{} 下载失败将进行重试, 当前重试次数为{}次", name, i);
        }
        log.error("{} 添加失败，疑似为坏种", name);
        MessageUtil.send(ConfigUtil.CONFIG, ani,
                StrFormatter.format("{} 添加失败，疑似为坏种", name),
                MessageEnum.ERROR);
    }

    /**
     * 生成 tvshow.info
     *
     * @param savePath
     * @param ani
     */
    public static synchronized void createTvShowNfo(String savePath, Ani ani) {
        Config config = ConfigUtil.CONFIG;

        Boolean tvShowNfo = config.getTvShowNfo();
        if (!tvShowNfo) {
            return;
        }

        Tmdb tmdb = ani.getTmdb();

        if (Objects.isNull(tmdb)) {
            return;
        }

        String tmdbId = tmdb.getId();

        if (StrUtil.isBlank(tmdbId)) {
            return;
        }
        String seasonName = config.getSeasonName();

        if (seasonName.equals("None")) {
            return;
        }

        ThreadUtil.execute(() -> {
            File tvshowFile = new File(new File(savePath).getParent() + "/tvshow.nfo");

            String tmdbGroupId = tmdb.getTmdbGroupId();
            tmdbGroupId = StrUtil.blankToDefault(tmdbGroupId, "");

            if (!tvshowFile.exists()) {
                String s = """
                        <?xml version="1.0" encoding="utf-8" standalone="yes"?>
                        <tvshow>
                            <tmdbid>{}</tmdbid>
                            <tmdbegid>{}</tmdbegid>
                        </tvshow>
                        """;
                FileUtil.writeUtf8String(StrFormatter.format(s, tmdbId, tmdbGroupId), tvshowFile);
                log.info("已创建 {}", tvshowFile);
                return;
            }

            if (StrUtil.isBlank(tmdbGroupId)) {
                return;
            }

            Document document = XmlUtil.readXML(tvshowFile);
            Element documentElement = document.getDocumentElement();
            NodeList tmdbegidNodeList = documentElement.getElementsByTagName("tmdbegid");
            for (int i = 0; i < tmdbegidNodeList.getLength(); i++) {
                Node item = tmdbegidNodeList.item(i);
                String textContent = item.getTextContent();
                if (tmdbGroupId.equals(textContent)) {
                    // 已包含有剧集组id
                    return;
                }
                documentElement.removeChild(item);
            }
            Element tmdbegidElement = document.createElement("tmdbegid");
            tmdbegidElement.setTextContent(tmdbGroupId);
            documentElement.appendChild(tmdbegidElement);

            FileUtil.writeUtf8String(XmlUtil.toStr(document), tvshowFile);
        });
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
        Ani ani = null;
        try {
            ani = findAniByDownloadPath(torrentsInfo);

            Set<String> allTags = Arrays.stream(TorrentsTags.values())
                    .map(TorrentsTags::getValue)
                    .collect(Collectors.toSet());

            String subgroup = tags
                    .stream()
                    .filter(s -> !allTags.contains(s))
                    .findFirst()
                    .orElse("");
            subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
            if (Objects.nonNull(ani)) {
                ani.setSubgroup(subgroup);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            AlistUtil.upload(torrentsInfo, ani);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            AlistUtil.refresh(torrentsInfo, ani);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        String text = StrFormatter.format("{} 下载完成", name);
        if (tags.contains(TorrentsTags.BACK_RSS.getValue())) {
            text = StrFormatter.format("(备用RSS) {}", text);
        }
        MessageUtil.send(ConfigUtil.CONFIG, ani, text, MessageEnum.DOWNLOAD_END);
    }

    /**
     * 根据任务反查订阅
     *
     * @param torrentsInfo
     * @return
     */
    public static synchronized Ani findAniByDownloadPath(TorrentsInfo torrentsInfo) {
        String downloadDir = torrentsInfo.getDownloadDir();
        return AniUtil.ANI_LIST
                .stream()
                .filter(ani -> {
                    String path = FilePathUtil.getAbsolutePath(TorrentUtil.getDownloadPath(ani).get(0));
                    return path.equals(downloadDir);
                })
                .map(ObjectUtil::clone)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断种子是否可以删除
     *
     * @param torrentsInfo
     * @return
     */
    public static Boolean isDelete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean awaitStalledUP = config.getAwaitStalledUP();

        TorrentsInfo.State state = torrentsInfo.getState();

        if (Objects.isNull(state)) {
            return false;
        }

        // 是否等待做种完毕
        if (awaitStalledUP) {
            return List.of(
                    TorrentsInfo.State.pausedUP.name(),
                    TorrentsInfo.State.stoppedUP.name()
            ).contains(state.name());
        }

        return List.of(
                TorrentsInfo.State.queuedUP.name(),
                TorrentsInfo.State.uploading.name(),
                TorrentsInfo.State.stalledUP.name(),
                TorrentsInfo.State.pausedUP.name(),
                TorrentsInfo.State.stoppedUP.name()
        ).contains(state.name());
    }


    /**
     * 删除已完成任务
     *
     * @param torrentsInfo 任务
     * @param forcedDelete 强制删除
     * @param deleteFiles  删除本地文件
     */
    public static synchronized Boolean delete(TorrentsInfo torrentsInfo, Boolean forcedDelete, Boolean deleteFiles) {
        Config config = ConfigUtil.CONFIG;
        Boolean delete = config.getDelete();

        String name = torrentsInfo.getName();

        if (forcedDelete) {
            log.info("删除任务 {}", name);
        } else {
            if (!isDelete(torrentsInfo)) {
                return false;
            }
            if (!delete) {
                return false;
            }
            log.info("删除已完成任务 {}", name);
        }
        ThreadUtil.sleep(500);
        List<String> files = torrentsInfo.getFiles().get();

        Boolean b = baseDownload.delete(torrentsInfo, deleteFiles);
        if (!b) {
            log.error("删除任务失败 {}", name);
            return false;
        }
        log.info("删除任务成功 {}", name);
        if (!deleteFiles) {
            return true;
        }
        // 清理空文件夹
        ClearCacheAction.clearParentFile(new File(torrentsInfo.getDownloadDir() + "/" + name));
        return true;
    }


    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    public static synchronized Boolean delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean deleteFiles = config.getDeleteFiles();
        Boolean alist = config.getAlist();
        if (!deleteFiles || !alist) {
            return delete(torrentsInfo, false, false);
        }
        // 开启 alist上传 后删除源文件的行为需要等待 alist 上传完成
        if (torrentsInfo.getTags().contains(TorrentsTags.UPLOAD_COMPLETED.getValue())) {
            return delete(torrentsInfo, false, true);
        }
        return false;
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

        List<String> tags = torrentsInfo.getTags();
        if (tags.contains(TorrentsTags.RENAME.getValue())) {
            return;
        }

        ThreadUtil.sleep(1000);
        baseDownload.rename(torrentsInfo);
        addTags(torrentsInfo, TorrentsTags.RENAME.getValue());
    }

    public static Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        if (StrUtil.isBlank(tags)) {
            return false;
        }
        String name = torrentsInfo.getName();
        log.debug("添加标签 {} {}", name, tags);
        boolean b = false;
        try {
            b = baseDownload.addTags(torrentsInfo, tags);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (b) {
            log.debug("添加标签成功 {} {}", name, tags);
        } else {
            log.error("添加标签失败 {} {}", name, tags);
        }
        return b;
    }


    /**
     * 修改保存位置
     *
     * @param torrentsInfo
     * @param path
     */
    public static void setSavePath(TorrentsInfo torrentsInfo, String path) {
        if (StrUtil.isBlank(path)) {
            return;
        }
        try {
            log.info("修改保存位置 {} ==> {}", torrentsInfo.getName(), path);
            baseDownload.setSavePath(torrentsInfo, path);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
        log.info("下载工具 {}", download);
    }

    /**
     * 通过种子获取到磁力链接
     *
     * @param file
     * @return
     */
    public static synchronized String getMagnet(File file) {
        String hexHash = FileUtil.mainName(file);
        if (file.length() < 1) {
            return StrFormatter.format("magnet:?xt=urn:btih:{}", hexHash);
        }
        if (FileUtil.extName(file).equals("txt")) {
            return FileUtil.readUtf8String(file);
        }
        try {
            TorrentFile torrentFile = new TorrentFile(file);
            hexHash = torrentFile.getHexHash();
        } catch (Exception e) {
            log.error("转换种子为磁力链接时出现错误 {}", FilePathUtil.getAbsolutePath(file));
            log.error(e.getMessage(), e);
        }
        return StrFormatter.format("magnet:?xt=urn:btih:{}", hexHash);
    }

}
