package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.google.gson.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TorrentUtil {

    private static final Log LOG = Log.get(TorrentUtil.class);

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
            LOG.warn("qBittorrent 未配置完成");
            return false;
        }

        try {
            return HttpRequest.post(host + "/api/v2/auth/login")
                    .form("username", username)
                    .form("password", password)
                    .setFollowRedirects(true)
                    .thenFunction(res -> {
                        if (!res.isOk() || !res.body().equals("Ok.")) {
                            LOG.error("登录 qBittorrent 失败");
                            return false;
                        }
                        return true;
                    });
        } catch (Exception e) {
            LOG.error("登录 qBittorrent 失败 {}", e.getMessage());
        }
        return false;
    }

    /**
     * 下载动漫
     *
     * @param ani
     * @param items
     */
    public static synchronized void downloadAni(Ani ani, List<Item> items) {
        Config config = ConfigUtil.getCONFIG();
        String downloadPath = config.getDownloadPath();

        Integer season = ani.getSeason();
        String title = ani.getTitle();

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();

        LOG.debug("{} 共 {} 个", title, items.size());
        for (Item item : items) {
            LOG.debug(JSONUtil.formatJsonStr(GSON.toJson(item)));
            String reName = item.getReName();
            File torrent = getTorrent(item);

            // 已经下载过
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos.stream()
                    .filter(torrentsInfo ->
                            StrUtil.equalsIgnoreCase(FileUtil.mainName(torrent), torrentsInfo.getHash())
                    )
                    .findFirst();
            if (optionalTorrentsInfo.isPresent()) {
                LOG.info("已有下载任务 {}", reName);
                TorrentsInfo torrentsInfo = optionalTorrentsInfo.get();
                rename(torrentsInfo, reName);
                delete(torrentsInfo);
                continue;
            }
            // 已经下载过
            if (torrent.exists()) {
                LOG.debug("种子记录已存在 {}", reName);
                continue;
            }

            // 未开启rename不进行检测
            if (itemDownloaded(ani, item)) {
                LOG.debug("本地文件已存在 {}", reName);
                continue;
            }
            LOG.info("添加下载 {}", reName);
            File saveTorrent = saveTorrent(item);

            String savePath = StrFormatter.format("{}/{}/Season {}", downloadPath, title, season);
            download(reName, savePath, saveTorrent);
        }
    }

    public static File getTorrent(Item item) {
        String torrent = item.getTorrent();

        File configDir = ConfigUtil.getConfigDir();
        File torrents = new File(configDir + File.separator + "torrents");
        FileUtil.mkdir(torrents);
        File torrentFile = new File(torrent);
        return new File(torrents + File.separator + torrentFile.getName());
    }

    /**
     * 下载种子文件
     *
     * @param item
     */
    public static File saveTorrent(Item item) {
        String torrent = item.getTorrent();
        String reName = item.getReName();

        LOG.info("下载种子 {}", reName);
        File saveTorrentFile = getTorrent(item);
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
        HttpRequest.post(host + "/api/v2/torrents/add")
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
    }

    /**
     * 获取任务列表
     *
     * @return
     */
    public static List<TorrentsInfo> getTorrentsInfos() {
        Config config = ConfigUtil.getCONFIG();
        String host = config.getHost();
        return HttpRequest.get(host + "/api/v2/torrents/info")
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
            LOG.info("已下载 {}", reName);
            // 保存 torrent 下次只校验 torrent 是否存在 ， 可以将config设置到固态硬盘，防止一直唤醒机械硬盘
            saveTorrent(item);
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
        if (EnumUtil.equals(state, TorrentsInfo.State.pausedUP.name())) {
            LOG.info("删除已完成任务 {}", name);
            HttpRequest.post(host + "/api/v2/torrents/delete")
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
        List<String> nameList = HttpRequest.get(host + "/api/v2/torrents/files")
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

            LOG.info("重命名 {} ==> {}", name, newPath);

            HttpRequest.post(host + "/api/v2/torrents/renameFile")
                    .form("hash", hash)
                    .form("oldPath", name)
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
        }

    }

}
