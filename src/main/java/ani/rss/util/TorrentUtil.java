package ani.rss.util;

import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TorrentUtil {

    @Setter
    private static BaseDownload baseDownload;

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();


    /**
     * 下载动漫
     *
     * @param ani
     */
    public static synchronized void downloadAni(Ani ani) {
        Config config = ConfigUtil.CONFIG;
        Boolean autoDisabled = config.getAutoDisabled();
        Integer downloadCount = config.getDownloadCount();
        Boolean ova = ani.getOva();

        String title = ani.getTitle();
        Integer season = ani.getSeason();

        Set<String> hashList = getTorrentsInfos()
                .stream().map(TorrentsInfo::getHash)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int currentDownloadCount = 0;
        List<Item> items = AniUtil.getItems(ani);
        log.debug("{} 共 {} 个", title, items.size());


        long count = getTorrentsInfos()
                .stream()
                .filter(it -> !EnumUtil.equalsIgnoreCase(it.getState(), TorrentsInfo.State.pausedUP.name()))
                .count();

        for (Item item : items) {
            log.debug(JSONUtil.formatJsonStr(GSON.toJson(item)));
            String reName = item.getReName();
            File torrent = getTorrent(ani, item);
            String hash = FileUtil.mainName(torrent)
                    .trim().toLowerCase();

            // 已经下载过
            if (hashList.contains(hash)) {
                log.debug("已有下载任务 {}", reName);
                currentDownloadCount++;
                continue;
            }

            // 已经下载过
            if (torrent.exists()) {
                log.debug("种子记录已存在 {}", reName);
                currentDownloadCount++;
                continue;
            }

            // 未开启rename不进行检测
            if (itemDownloaded(ani, item)) {
                log.debug("本地文件已存在 {}", reName);
                currentDownloadCount++;
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
            String savePath = getDownloadPath(ani).get(0).toString();
            download(reName, savePath, saveTorrent, ova);
            currentDownloadCount++;
            count++;
        }
        ani.setCurrentEpisodeNumber(items.size());
        if (!autoDisabled) {
            return;
        }
        AniUtil.getBangumiInfo(ani, false, true);
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

    public static File getTorrentDir(Ani ani) {
        String title = ani.getTitle();
        Boolean ova = ani.getOva();
        Integer season = ani.getSeason();

        File configDir = ConfigUtil.getConfigDir();

        File torrents = new File(StrFormatter.format("{}/torrents/{}/Season {}", configDir, title, season));
        if (ova) {
            torrents = new File(StrFormatter.format("{}/torrents/{}", configDir, title));
        }
        FileUtil.mkdir(torrents);
        return torrents;
    }

    public static File getTorrent(Ani ani, Item item) {
        String torrent = item.getTorrent();
        File torrents = getTorrentDir(ani);
        File torrentFile = new File(torrent);
        return new File(torrents + File.separator + torrentFile.getName());
    }

    /**
     * 下载种子文件
     *
     * @param item
     */
    public static File saveTorrent(Ani ani, Item item) {
        return saveTorrent(ani, item, 0);
    }

    /**
     * 下载种子文件
     *
     * @param item
     */
    public static File saveTorrent(Ani ani, Item item, Integer count) {
        String torrent = item.getTorrent();
        String reName = item.getReName();

        log.info("下载种子 {}", reName);
        File saveTorrentFile = getTorrent(ani, item);
        if (saveTorrentFile.exists()) {
            return saveTorrentFile;
        }

        if (count > 10) {
            return saveTorrentFile;
        }

        if (count > 0) {
            ThreadUtil.sleep(1000);
        }

        int i = count + 1;
        return HttpReq.get(torrent, true)
                .thenFunction(res -> {
                    if (!res.isOk()) {
                        return saveTorrent(ani, item, i);
                    }
                    FileUtil.writeFromStream(res.bodyStream(), saveTorrentFile, true);
                    return saveTorrentFile;
                });
    }

    /**
     * 判断是否已经下载过
     *
     * @param ani
     * @param item
     * @return
     */
    public static Boolean itemDownloaded(Ani ani, Item item) {
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
        Integer episode = item.getEpisode();

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
        for (TorrentsInfo torrentsInfo : torrentsInfos) {
            String name = torrentsInfo.getName();
            if (name.equalsIgnoreCase(reName)) {
                log.info("已存在下载任务 {}", reName);
                saveTorrent(ani, item);
                return true;
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
                        return BaseDownload.videoFormat.contains(FileUtil.extName(file));
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
                    String s = "S(\\d+)E(\\d+)$";
                    if (!ReUtil.contains(s, mainName)) {
                        return false;
                    }

                    String seasonStr = ReUtil.get(s, mainName, 1);

                    String episodeStr = ReUtil.get(s, mainName, 2);

                    if (StrUtil.isBlank(seasonStr) || StrUtil.isBlank(episodeStr)) {
                        return false;
                    }
                    return season == Integer.parseInt(seasonStr) && episode == Integer.parseInt(episodeStr);
                })) {
            log.info("已下载 {}", reName);
            // 保存 torrent 下次只校验 torrent 是否存在 ， 可以将config设置到固态硬盘，防止一直唤醒机械硬盘
            saveTorrent(ani, item);
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
        String title = ani.getTitle().trim();
        Integer season = ani.getSeason();
        Boolean ova = ani.getOva();

        Config config = ConfigUtil.CONFIG;
        String downloadPath = config.getDownloadPath();
        String ovaDownloadPath = config.getOvaDownloadPath();
        Boolean acronym = config.getAcronym();
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
        }
        if (ova) {
            return List.of(new File(downloadPath + "/" + title));
        }
        File file = new File(StrFormatter.format("{}/{}/Season {}", downloadPath, title, season));
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
     * @param name
     * @param savePath
     * @param torrentFile
     */
    public static synchronized void download(String name, String savePath, File torrentFile, Boolean ova) {
        if (!torrentFile.exists()) {
            log.error("种子下载出现问题 {} {}", name, torrentFile.getAbsolutePath());
            MailUtils.send(StrFormatter.format("种子下载出现问题 {} {}", name, torrentFile.getAbsolutePath()));
            return;
        }
        savePath = savePath.replace("\\", "/");
        MailUtils.send(StrFormatter.format("{} 已更新", name));
        if (baseDownload.download(name, savePath, torrentFile, ova)) {
            return;
        }
        log.error("{} 添加失败，疑似为坏种", name);
        MailUtils.send(StrFormatter.format("{} 添加失败，疑似为坏种", name));
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
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    public static synchronized void delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean delete = config.getDelete();

        TorrentsInfo.State state = torrentsInfo.getState();
        String name = torrentsInfo.getName();
        if (!EnumUtil.equalsIgnoreCase(state, TorrentsInfo.State.pausedUP.name())) {
            return;
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
        if (rename) {
            ThreadUtil.sleep(1000);
            baseDownload.rename(torrentsInfo);
        }
    }

    public static void load() {
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

}
