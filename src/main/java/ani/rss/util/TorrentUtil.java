package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.log.Log;
import com.google.gson.*;

import java.io.File;
import java.util.*;

public class TorrentUtil {
    private final static Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private static final Log log = Log.get(TorrentUtil.class);

    private final static String host = "https://www.wushuo.fun:8844";
    public static String downloadPath = "/downloads";

    public static synchronized void download(Ani ani, List<Item> items) {
        List<TorrentsInfo> torrentsInfos = HttpRequest.get(host + "/api/v2/torrents/info")
                .thenFunction(res -> {
                    List<TorrentsInfo> torrentsInfoList = new ArrayList<>();
                    JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        TorrentsInfo torrentsInfo = gson.fromJson(jsonObject, TorrentsInfo.class);
                        torrentsInfoList.add(torrentsInfo);
                    }
                    return torrentsInfoList;
                });

        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        File torrents = new File(config + File.separator + "torrents");
        FileUtil.mkdir(torrents);

        Integer season = ani.getSeason();
        for (Item item : items) {
            String reName = item.getReName();
            String torrent = item.getTorrent();
            File torrentFile = new File(torrent);
            File saveTorrentFile = new File(torrents + File.separator + torrentFile.getName());

            // 已经下载过
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos.stream().filter(torrentsInfo -> (torrentsInfo.getHash() + ".torrent").equals(torrentFile.getName()))
                    .findFirst();
            if (optionalTorrentsInfo.isPresent()) {
                log.info("{} 已有下载任务", reName);
                TorrentsInfo torrentsInfo = optionalTorrentsInfo.get();
                TorrentsInfo.State state = torrentsInfo.getState();
                String hash = torrentsInfo.getHash();
                // 重命名
                HttpRequest.get(host + "/api/v2/torrents/files")
                        .form("hash", hash)
                        .then(res -> {
                            JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);
                            for (JsonElement jsonElement : jsonElements) {
                                String name = jsonElement.getAsJsonObject().get("name").getAsString();
                                String ext = FileUtil.extName(name);
                                String newPath = reName + "." + ext;
                                if (name.equals(newPath)) {
                                    // 下载完成后自动删除任务
                                    if (EnumUtil.equals(state, "pausedUP") || EnumUtil.equals(state, "stalledUP")) {
                                        HttpRequest.post(host + "/api/v2/torrents/delete")
                                                .form("hashes", hash)
                                                .form("deleteFiles", false)
                                                .thenFunction(HttpResponse::isOk);
                                    }
                                    return;
                                }
                                HttpRequest.post(host + "/api/v2/torrents/renameFile")
                                        .form("hash", hash)
                                        .form("oldPath", name)
                                        .form("newPath", newPath)
                                        .thenFunction(HttpResponse::isOk);
                            }
                        });
                continue;
            }

            // 已经下载过
            if (saveTorrentFile.exists()) {
                continue;
            }

            String savePath = downloadPath + File.separator + ani.getTitle() + "/S" + String.format("%02d", season);
            List<File> files = new ArrayList<>();

            File sFile = new File(savePath);
            File seasonFile = new File(downloadPath + File.separator + ani.getTitle() + "/Season " + season);
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
                log.info("{} 已下载", reName);
                // 保存 torrent 下次只校验 torrent 是否存在 ， 可以将config设置到固态硬盘，防止一直唤醒机械硬盘
                byte[] bytes = HttpRequest.get(torrent).thenFunction(HttpResponse::bodyBytes);
                FileUtil.writeBytes(bytes, saveTorrentFile);
                continue;
            }
            log.info("{} 下载", reName);

            byte[] bytes = HttpRequest.get(torrent).thenFunction(HttpResponse::bodyBytes);

            FileUtil.writeBytes(bytes, saveTorrentFile);
            HttpRequest.post(host + "/api/v2/torrents/add")
                    .form("addToTopOfQueue", false)
                    .form("autoTMM", false)
                    .form("contentLayout", "Original")
                    .form("dlLimit", 0)
                    .form("firstLastPiecePrio", false)
                    .form("paused", false)
                    .form("rename", reName)
                    .form("savepath", savePath)
                    .form("sequentialDownload", false)
                    .form("skip_checking", false)
                    .form("stopCondition", "None")
                    .form("upLimit", 102400)
                    .form("useDownloadPath", false)
                    .form("torrents", bytes, torrentFile.getName())
                    .thenFunction(HttpResponse::isOk);
        }
    }
}
