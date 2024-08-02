package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TorrentUtil {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    /**
     * 登录 qBittorrent
     *
     * @return
     */
    public static Boolean login() {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        String username = config.getUsername();
        String password = config.getPassword();
        String downloadPath = config.getDownloadPath();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(username)
                || StrUtil.isBlank(password) || StrUtil.isBlank(downloadPath)) {
            log.warn("qBittorrent 未配置完成");
            return false;
        }

        try {
            return HttpReq.post(host + "/api/v2/auth/login", false)
                    .form("username", username)
                    .form("password", password)
                    .setFollowRedirects(true)
                    .thenFunction(res -> {
                        if (!res.isOk() || !res.body().equals("Ok.")) {
                            log.error("登录 qBittorrent 失败");
                            return false;
                        }
                        return true;
                    });
        } catch (Exception e) {
            log.error("登录 qBittorrent 失败 {}", e.getMessage());
        }
        return false;
    }

    /**
     * 下载动漫
     *
     * @param ani
     */
    public static synchronized void downloadAni(Ani ani) {
        Config config = ConfigUtil.getCONFIG();
        String downloadPath = config.getDownloadPath();
        Integer downloadCount = config.getDownloadCount();

        Integer season = ani.getSeason();
        String title = ani.getTitle();

        Set<String> hashList = getTorrentsInfos()
                .stream().map(TorrentsInfo::getHash)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Item> items = AniUtil.getItems(ani);
        log.debug("{} 共 {} 个", title, items.size());
        for (Item item : items) {
            log.debug(JSONUtil.formatJsonStr(GSON.toJson(item)));
            String reName = item.getReName();
            File torrent = getTorrent(ani, item);
            String hash = FileUtil.mainName(torrent)
                    .trim().toLowerCase();

            // 已经下载过
            if (hashList.contains(hash)) {
                log.debug("已有下载任务 {}", reName);
                continue;
            }

            // 已经下载过
            if (torrent.exists()) {
                log.debug("种子记录已存在 {}", reName);
                continue;
            }

            // 未开启rename不进行检测
            if (itemDownloaded(ani, item)) {
                log.debug("本地文件已存在 {}", reName);
                continue;
            }

            long count = getTorrentsInfos()
                    .stream()
                    .filter(it -> !EnumUtil.equalsIgnoreCase(it.getState(), TorrentsInfo.State.pausedUP.name()))
                    .count();

            // 同时下载数量限制
            if (downloadCount > 0 && count >= downloadCount) {
                log.debug("达到同时下载数量限制 {}", downloadCount);
                continue;
            }

            log.info("添加下载 {}", reName);
            File saveTorrent = saveTorrent(ani, item);

            String savePath = StrFormatter.format("{}/{}/Season {}", downloadPath, title, season);
            download(reName, savePath, saveTorrent);
        }
    }

    public static File getTorrent(Ani ani, Item item) {
        String title = ani.getTitle();
        Integer season = ani.getSeason();
        String torrent = item.getTorrent();

        File configDir = ConfigUtil.getConfigDir();

        File torrents = new File(StrFormatter.format("{}/torrents/{}/Season {}", configDir, title, season));
        FileUtil.mkdir(torrents);
        File torrentFile = new File(torrent);
        return new File(torrents + File.separator + torrentFile.getName());
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
        HttpUtil.downloadFile(torrent, saveTorrentFile);
        return saveTorrentFile;
    }

    /**
     * 下载
     *
     * @param name
     * @param savePath
     * @param torrentFile
     */
    public static void download(String name, String savePath, File torrentFile) {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        Integer downloadCount = config.getDownloadCount();
        HttpReq.post(host + "/api/v2/torrents/add", false)
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("contentLayout", "Original")
                .form("dlLimit", 0)
                .form("firstLastPiecePrio", false)
                .form("paused", false)
                .form("rename", name)
                .form("savepath", savePath)
                .form("sequentialDownload", false)
                .form("skip_checking", false)
                .form("stopCondition", "None")
                .form("upLimit", 102400)
                .form("useDownloadPath", false)
                .form("torrents", torrentFile)
                .form("tags", "ani-rss")
                .thenFunction(HttpResponse::isOk);

        MailUtils.send(StrFormatter.format("{} 已更新", name));

        if (downloadCount < 1) {
            return;
        }
        String hash = FileUtil.mainName(torrentFile);
        // 等待任务添加完成 最多等待10次检测
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(1000);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                if (hash.equals(torrentsInfo.getHash())) {
                    return;
                }
            }
        }
    }

    /**
     * 获取任务列表
     *
     * @return
     */
    public static synchronized List<TorrentsInfo> getTorrentsInfos() {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        return HttpReq.get(host + "/api/v2/torrents/info", false)
                .thenFunction(res -> {
                    List<TorrentsInfo> torrentsInfoList = new ArrayList<>();
                    JsonArray jsonElements = GSON.fromJson(res.body(), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        TorrentsInfo torrentsInfo = GSON.fromJson(jsonObject, TorrentsInfo.class);
                        String tags = torrentsInfo.getTags();
                        if (StrUtil.isBlank(tags)) {
                            continue;
                        }
                        // 包含标签
                        if (StrUtil.split(tags, ",", true, true).contains("ani-rss")) {
                            torrentsInfoList.add(torrentsInfo);
                        }
                    }
                    return torrentsInfoList;
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
        Config config = ConfigUtil.getCONFIG();
        Boolean rename = config.getRename();
        if (!rename) {
            return false;
        }

        String downloadPath = config.getDownloadPath();
        Boolean fileExist = config.getFileExist();
        if (!fileExist || StrUtil.isBlank(downloadPath)) {
            return false;
        }

        Integer season = ani.getSeason();
        String reName = item.getReName();

        String savePath = downloadPath + File.separator + ani.getTitle() + "/Season " + season;
        List<File> files = new ArrayList<>();

        File sFile = new File(savePath);
        File seasonFile = new File(downloadPath + File.separator + ani.getTitle() + "/S" + String.format("%02d", season));
        if (sFile.exists()) {
            files.addAll(Arrays.asList(ObjectUtil.defaultIfNull(sFile.listFiles(), new File[]{})));
        }
        if (seasonFile.exists()) {
            files.addAll(Arrays.asList(ObjectUtil.defaultIfNull(seasonFile.listFiles(), new File[]{})));
        }

        if (files.stream()
                .filter(File::isFile)
                .filter(file -> List.of("mp4", "mkv", "avi").contains(FileUtil.extName(file)))
                .anyMatch(file -> FileUtil.getPrefix(file).equals(reName))) {
            log.info("已下载 {}", reName);
            // 保存 torrent 下次只校验 torrent 是否存在 ， 可以将config设置到固态硬盘，防止一直唤醒机械硬盘
            saveTorrent(ani, item);
            return true;
        }

        return false;
    }

    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    public static void delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        Boolean delete = config.getDelete();
        if (!delete) {
            return;
        }
        String hash = torrentsInfo.getHash();
        TorrentsInfo.State state = torrentsInfo.getState();
        String name = torrentsInfo.getName();
        // 下载完成后自动删除任务
        if (EnumUtil.equalsIgnoreCase(state, TorrentsInfo.State.pausedUP.name())) {
            log.info("删除已完成任务 {}", name);
            HttpReq.post(host + "/api/v2/torrents/delete", false)
                    .form("hashes", hash)
                    .form("deleteFiles", false)
                    .thenFunction(HttpResponse::isOk);
        }
    }

    /**
     * 重命名
     *
     * @param torrentsInfo
     * @param reName
     */
    public static void rename(TorrentsInfo torrentsInfo, String reName) {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        Boolean rename = config.getRename();
        if (!rename) {
            return;
        }
        String hash = torrentsInfo.getHash();
        List<String> nameList = HttpReq.get(host + "/api/v2/torrents/files", false)
                .form("hash", hash)
                .thenFunction(res -> {
                    JsonArray jsonElements = GSON.fromJson(res.body(), JsonArray.class);

                    List<String> names = new ArrayList<>();
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        String name = jsonObject.get("name").getAsString();
                        names.add(name);
                    }
                    return names;
                });

        List<String> newNames = new ArrayList<>();

        for (String name : nameList) {
            String ext = FileUtil.extName(name);
            String newPath = reName;
            if (List.of("mp4", "mkv", "avi").contains(ext)) {
                newPath = newPath + "." + ext;
            } else if ("ass".equalsIgnoreCase(ext)) {
                String s = FileUtil.extName(FileUtil.mainName(name));
                if (StrUtil.isNotBlank(s)) {
                    newPath = newPath + "." + s;
                }
                newPath = newPath + "." + ext;
            } else {
                continue;
            }

            if (nameList.contains(newPath)) {
                continue;
            }
            if (newNames.contains(newPath)) {
                continue;
            }
            newNames.add(newPath);

            if (name.equals(newPath)) {
                continue;
            }

            log.info("重命名 {} ==> {}", name, newPath);

            HttpReq.post(host + "/api/v2/torrents/renameFile", false)
                    .form("hash", hash)
                    .form("oldPath", name)
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
        }

    }

}
