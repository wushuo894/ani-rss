package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * qBittorrent
 */
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
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            log.error("登录 qBittorrent 失败 {}", message);
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
                        torrentsInfo.setDownloadDir(jsonObject.get("content_path").getAsString());
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
    public Boolean download(Item item, String savePath, File torrentFile, Boolean ova) {
        String name = item.getReName();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
        String host = config.getHost();
        Boolean qbUseDownloadPath = config.getQbUseDownloadPath();
        HttpRequest httpRequest = HttpReq.post(host + "/api/v2/torrents/add", false)
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
                .form("useDownloadPath", qbUseDownloadPath)
                .form("tags", "ani-rss," + subgroup);

        String extName = FileUtil.extName(torrentFile);
        if ("txt".equals(extName)) {
            httpRequest.form("urls", FileUtil.readUtf8String(torrentFile));
        } else {
            httpRequest.form("torrents", torrentFile);
        }
        httpRequest.thenFunction(HttpResponse::isOk);

        String hash = FileUtil.mainName(torrentFile);
        Boolean watchErrorTorrent = config.getWatchErrorTorrent();

        if (!watchErrorTorrent) {
            ThreadUtil.sleep(3000);
            return true;
        }


        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(3000);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos
                    .stream()
                    .filter(torrentsInfo ->
                            torrentsInfo.getHash().equals(hash) ||
                                    torrentsInfo.getName().equals(name)
                    )
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
        String reName = torrentsInfo.getName();
        if (!ReUtil.contains(StringEnum.SEASON_REG, reName)) {
            return;
        }

        String hash = torrentsInfo.getHash();

        if (StrUtil.isBlank(reName)) {
            return;
        }

        String host = config.getHost();
        Integer renameMinSize = config.getRenameMinSize();

        List<FileEntity> fileEntityList = HttpReq.get(host + "/api/v2/torrents/files", false)
                .form("hash", hash)
                .thenFunction(res -> gson.fromJson(res.body(), JsonArray.class)
                        .asList()
                        .stream()
                        .map(jsonElement -> gson.fromJson(jsonElement, FileEntity.class))
                        .filter(fileEntity -> {
                            String name = fileEntity.getName();
                            String extName = FileUtil.extName(name);
                            if (StrUtil.isBlank(extName)) {
                                return false;
                            }
                            Long size = fileEntity.getSize();
                            if (size < 1) {
                                return false;
                            }
                            return videoFormat.contains(extName) || subtitleFormat.contains(extName);
                        })
                        .sorted(Comparator.comparingLong(fileEntity -> Long.MAX_VALUE - fileEntity.getSize()))
                        .collect(Collectors.toList()));

        Assert.notEmpty(fileEntityList, "{} 磁力链接还在获取原数据中", hash);

        long videoCount = fileEntityList.stream()
                .map(FileEntity::getName)
                .map(FileUtil::extName)
                .filter(StrUtil::isNotBlank)
                .filter(videoFormat::contains)
                .count();

        // 重命名文件大小限制
        List<FileEntity> newFileEntityList = fileEntityList.stream()
                .filter(fileEntity -> {
                    Long size = fileEntity.getSize();
                    String name = fileEntity.getName();
                    String extName = FileUtil.extName(name);
                    // 排除字幕
                    if (subtitleFormat.contains(extName)) {
                        return true;
                    }
                    // 大小限制为0时不启用
                    if (renameMinSize < 1) {
                        return true;
                    }
                    return renameMinSize <= size / 1024 / 1024;
                }).collect(Collectors.toList());

        // 过滤结果不为空
        if (!newFileEntityList.isEmpty() && videoCount > 1) {
            fileEntityList = newFileEntityList;
        }

        List<String> names = fileEntityList.stream().map(FileEntity::getName)
                .collect(Collectors.toList());

        List<String> newNames = new ArrayList<>();

        for (String name : names) {
            String newPath = getFileReName(name, reName);

            if (names.contains(newPath)) {
                continue;
            }
            if (newNames.contains(newPath)) {
                continue;
            }
            newNames.add(newPath);

            // 文件名未发生改变
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
        }

        // 清理空文件夹
        String downloadDir = torrentsInfo.getDownloadDir();
        File file = new File(downloadDir);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            return;
        }
        for (File itemFile : ObjUtil.defaultIfNull(file.listFiles(), new File[]{})) {
            if (itemFile.isFile()) {
                continue;
            }
            if (ArrayUtil.isEmpty(itemFile.listFiles())) {
                log.info("删除空文件夹: {}", itemFile);
                try {
                    FileUtil.del(itemFile);
                } catch (Exception e) {
                    log.error("删除空文件夹失败: {}", itemFile);
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Data
    @Accessors(chain = true)
    static class FileEntity {
        private String name;
        private Long size;
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        String host = config.getHost();
        String hash = torrentsInfo.getHash();
        return HttpReq.post(host + "/api/v2/torrents/addTags", false)
                .form("hashes", hash)
                .form("tags", tags)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public void updateTrackers(Set<String> trackers) {
        String host = config.getHost();
        JsonObject preferences = HttpReq.get(host + "/api/v2/app/preferences", false)
                .thenFunction(res -> {
                    int status = res.getStatus();
                    boolean ok = res.isOk();
                    Assert.isTrue(ok, "更新trackers失败 {}", status);
                    String body = res.body();
                    return gson.fromJson(body, JsonObject.class);
                });

        preferences.addProperty("add_trackers", CollUtil.join(trackers, "\n"));

        HttpReq.post(host + "/api/v2/app/setPreferences", false)
                .form("json", gson.toJson(preferences))
                .then(res -> {
                    if (res.isOk()) {
                        log.info("qBittorrent 更新Trackers完成 共{}条", trackers.size());
                        return;
                    }
                    log.error("qBittorrent 更新Trackers失败 {}", res.getStatus());
                });

    }


}
