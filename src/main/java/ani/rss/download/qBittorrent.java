package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.HttpReq;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class qBittorrent implements BaseDownload {
    private Config config;

    @Override
    public Boolean login(Config config) {
        this.config = config;
        String host = config.getHost();
        String username = config.getUsername();
        String password = config.getPassword();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(username)
                || StrUtil.isBlank(password)) {
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

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        String host = config.getHost();
        return HttpReq.get(host + "/api/v2/torrents/info", false)
                .thenFunction(res -> {
                    List<TorrentsInfo> torrentsInfoList = new ArrayList<>();
                    JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        TorrentsInfo torrentsInfo = gson.fromJson(jsonObject, TorrentsInfo.class);
                        String tags = torrentsInfo.getTags();
                        if (StrUtil.isBlank(tags)) {
                            continue;
                        }
                        // 包含标签
                        if (StrUtil.split(tags, ",", true, true).contains(tag)) {
                            torrentsInfoList.add(torrentsInfo);
                        }
                    }
                    return torrentsInfoList;
                });
    }

    @Override
    public Boolean download(String name, String savePath, File torrentFile, Boolean ova) {
        String host = config.getHost();
        Integer renameSleep = config.getRenameSleep();
        Boolean qbRenameTitle = config.getQbRenameTitle();
        Boolean qbUseDownloadPath = config.getQbUseDownloadPath();
        HttpRequest httpRequest = HttpReq.post(host + "/api/v2/torrents/add", false)
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("contentLayout", "Original")
                .form("dlLimit", 0)
                .form("firstLastPiecePrio", false)
                .form("paused", false)
                .form("rename", qbRenameTitle ? name : "")
                .form("savepath", savePath)
                .form("sequentialDownload", false)
                .form("skip_checking", false)
                .form("stopCondition", "None")
                .form("upLimit", 102400)
                .form("useDownloadPath", Boolean.TRUE.equals(qbUseDownloadPath))
                .form("tags", "ani-rss");

        String extName = FileUtil.extName(torrentFile);
        if ("txt".equals(extName)) {
            httpRequest.form("urls", FileUtil.readUtf8String(torrentFile));
        } else {
            httpRequest.form("torrents", torrentFile);
        }
        httpRequest.thenFunction(HttpResponse::isOk);

        String hash = FileUtil.mainName(torrentFile);
        Boolean watchErrorTorrent = config.getWatchErrorTorrent();

        if (!qbRenameTitle && !"txt".equals(extName) && !ova) {
            renameCache.put(hash, name);
        }

        if (!watchErrorTorrent) {
            return true;
        }


        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(2000);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos
                    .stream()
                    .filter(torrentsInfo -> torrentsInfo.getHash().equals(hash))
                    .findFirst();
            if (optionalTorrentsInfo.isPresent()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void delete(TorrentsInfo torrentsInfo) {
        String host = config.getHost();
        String hash = torrentsInfo.getHash();
        HttpReq.post(host + "/api/v2/torrents/delete", false)
                .form("hashes", hash)
                .form("deleteFiles", false)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo) {
        Boolean qbRenameTitle = config.getQbRenameTitle();
        String reName = torrentsInfo.getName();
        if (!ReUtil.contains("S\\d+E\\d+$", reName) && qbRenameTitle) {
            return;
        }

        String hash = torrentsInfo.getHash();

        if (!qbRenameTitle) {
            reName = renameCache.get(hash);
        }
        if (StrUtil.isBlank(reName)) {
            return;
        }

        String host = config.getHost();


        List<String> nameList = HttpReq.get(host + "/api/v2/torrents/files", false)
                .form("hash", hash)
                .thenFunction(res -> {
                    JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);

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
            String newPath = getFileReName(name, reName);

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

            Boolean b = HttpReq.post(host + "/api/v2/torrents/renameFile", false)
                    .form("hash", hash)
                    .form("oldPath", name)
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
            Assert.isTrue(b, "重命名失败 {} ==> {}", name, newPath);
            renameCache.remove(hash);
        }
    }
}
