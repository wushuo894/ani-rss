package ani.rss.download;

import ani.rss.commons.ExceptionUtil;
import ani.rss.commons.FileUtil;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TorrentsTags;
import ani.rss.service.DownloadService;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
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

/**
 * qBittorrent
 */
@Slf4j
public class qBittorrent implements BaseDownload {
    private Config config;

    /**
     * 获取对应任务的文件列表
     *
     * @param torrentsInfo
     * @param filter       过滤出视频与字幕
     * @param config
     * @return
     */
    public static List<FileEntity> files(TorrentsInfo torrentsInfo, Boolean filter, Config config) {
        String hash = torrentsInfo.getHash();
        String host = config.getDownloadToolHost();

        return HttpReq.get(host + "/api/v2/torrents/files")
                .form("hash", hash)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJsonList(res.body(), FileEntity.class).stream()
                            .filter(fileEntity -> {
                                if (!filter) {
                                    return true;
                                }
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
                            .sorted((fileEntity1, fileEntity2) -> Long.compare(fileEntity2.getSize(), fileEntity1.getSize()))
                            .toList();
                });
    }

    @Override
    public Boolean login(Boolean test, Config config) {
        this.config = config;
        String host = config.getDownloadToolHost();
        String username = config.getDownloadToolUsername();
        String password = config.getDownloadToolPassword();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(username)
                || StrUtil.isBlank(password)) {
            log.warn("qBittorrent 未配置完成");
            return false;
        }

        try {
            if (!test) {
                // 校验当前登录状态
                Boolean isOk = HttpReq.post(host + "/api/v2/app/version")
                        .thenFunction(HttpResponse::isOk);
                if (isOk) {
                    return true;
                }
            }

            return HttpReq.post(host + "/api/v2/auth/login")
                    .form("username", username)
                    .form("password", password)
                    .disableCookie()
                    .thenFunction(res -> {
                        HttpReq.assertStatus(res);
                        String body = res.body();
                        Assert.isTrue("Ok.".equals(body), "body: {}", body);
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
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile, Boolean ova) {
        String name = item.getReName();
        String host = config.getDownloadToolHost();
        Boolean qbUseDownloadPath = config.getQbUseDownloadPath();

        List<String> tags = newTags(ani, item);

        Integer ratioLimit = config.getRatioLimit();
        Integer seedingTimeLimit = config.getSeedingTimeLimit();
        Integer inactiveSeedingTimeLimit = config.getInactiveSeedingTimeLimit();
        Boolean rename = config.getRename();

        Long upLimit = config.getUpLimit() * 1024;
        Long dlLimit = config.getDlLimit() * 1024;

        HttpRequest httpRequest = HttpReq.post(host + "/api/v2/torrents/add")
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("category", TorrentsTags.ANI_RSS.getValue())
                .form("contentLayout", "Original")
                .form("dlLimit", dlLimit)
                .form("firstLastPiecePrio", false)
                .form("rename", name)
                .form("savepath", savePath)
                .form("sequentialDownload", false)
                .form("skip_checking", false)
                .form("stopCondition", "None")
                .form("upLimit", upLimit)
                .form("useDownloadPath", qbUseDownloadPath)
                .form("tags", CollUtil.join(tags, ","))
                .form("ratioLimit", ratioLimit)
                .form("seedingTimeLimit", seedingTimeLimit)
                .form("inactiveSeedingTimeLimit", inactiveSeedingTimeLimit);

        String extName = FileUtil.extName(torrentFile);
        if ("txt".equals(extName)) {
            httpRequest
                    .form("paused", false)
                    .form("stopped", false)
                    .form("urls", FileUtil.readUtf8String(torrentFile));
        } else {
            if (torrentFile.length() > 0) {
                // 开启了重命名则在重命名后再开始下载
                httpRequest.form("paused", rename)
                        .form("stopped", rename)
                        .form("torrents", torrentFile);
            } else {
                httpRequest
                        .form("paused", false)
                        .form("stopped", false)
                        .form("urls", "magnet:?xt=urn:btih:" + FileUtil.mainName(torrentFile));
            }
        }
        httpRequest.thenFunction(HttpResponse::isOk);

        String hash = FileUtil.mainName(torrentFile);
        Boolean watchErrorTorrent = config.getWatchErrorTorrent();

        if (!watchErrorTorrent) {
            ThreadUtil.sleep(3000);
            return true;
        }


        for (int i = 0; i < 3; i++) {
            ThreadUtil.sleep(1000 * 10);
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

    /**
     * 开始下载
     *
     * @param torrentsInfo
     * @return
     */
    public static Boolean start(TorrentsInfo torrentsInfo, Config config) {
        String host = config.getDownloadToolHost();
        boolean b = HttpReq.post(host + "/api/v2/torrents/start")
                .form("hashes", torrentsInfo.getHash())
                .thenFunction(HttpResponse::isOk);
        if (b) {
            return true;
        }

        return HttpReq.post(host + "/api/v2/torrents/resume")
                .form("hashes", torrentsInfo.getHash())
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        String host = config.getDownloadToolHost();
        try {
            return HttpReq.get(host + "/api/v2/torrents/info")
                    .thenFunction(res -> {
                        List<TorrentsInfo> torrentsInfoList = new ArrayList<>();
                        JsonArray jsonElements = GsonStatic.fromJson(res.body(), JsonArray.class);
                        for (JsonElement jsonElement : jsonElements) {
                            JsonObject jsonObject = jsonElement.getAsJsonObject();
                            String tags = jsonObject.get("tags").getAsString();

                            if (StrUtil.isBlank(tags)) {
                                continue;
                            }

                            String hash = jsonObject.get("hash").getAsString();
                            String name = jsonObject.get("name").getAsString();
                            String savePath = jsonObject.get("save_path").getAsString();
                            long completed = jsonObject.get("completed").getAsLong();
                            long size = jsonObject.get("size").getAsLong();
                            JsonElement state = jsonObject.get("state");

                            List<String> tagList = StrUtil.split(tags, ",", true, true);

                            TorrentsInfo torrentsInfo = new TorrentsInfo();

                            torrentsInfo.setState(Objects.isNull(state) ?
                                    TorrentsInfo.State.downloading : EnumUtil.fromString(TorrentsInfo.State.class, state.getAsString(), TorrentsInfo.State.downloading)
                            );

                            torrentsInfo
                                    .progress(completed, size)
                                    .setName(name)
                                    .setHash(hash)
                                    .setDownloadDir(FileUtil.getAbsolutePath(savePath))
                                    .setTags(tagList)
                                    .setFiles(() ->
                                            files(torrentsInfo, true, config)
                                                    .stream()
                                                    .filter(fileEntity -> fileEntity.getPriority() > 0)
                                                    .map(FileEntity::getName)
                                                    .toList());
                            // 包含标签
                            if (tagList.contains(TorrentsTags.ANI_RSS.getValue())) {
                                torrentsInfoList.add(torrentsInfo);
                                continue;
                            }

                            JsonElement category = jsonObject.get("category");
                            if (Objects.isNull(category)) {
                                continue;
                            }
                            if (category.getAsString().equals(TorrentsTags.ANI_RSS.getValue())) {
                                torrentsInfoList.add(torrentsInfo);
                            }
                        }
                        return torrentsInfoList;
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles) {
        String host = config.getDownloadToolHost();
        String name = torrentsInfo.getName();
        String hash = torrentsInfo.getHash();
        try {
            List<FileEntity> files = files(torrentsInfo, false, config);
            boolean b = HttpReq.post(host + "/api/v2/torrents/delete")
                    .form("hashes", hash)
                    .form("deleteFiles", deleteFiles)
                    .thenFunction(HttpResponse::isOk);
            if (!b) {
                return false;
            }

            // 剧场版不用进行残留的文件夹清理
            if (!ReUtil.contains(StringEnum.SEASON_REG, name)) {
                return true;
            }

            String downloadDir = torrentsInfo.getDownloadDir();

            List<File> dirList = files.stream()
                    .map(FileEntity::getName)
                    .map(File::new)
                    .map(File::getParent)
                    .filter(StrUtil::isNotBlank)
                    .map(s -> downloadDir + "/" + s)
                    .distinct()
                    .map(File::new)
                    .filter(File::exists)
                    .filter(File::isDirectory)
                    .toList();

            // 清空剩余文件夹
            for (File file : dirList) {
                log.info("删除剩余文件夹: {}", file);
                try {
                    FileUtil.del(file);
                } catch (Exception e) {
                    log.info("删除失败: {}", file);
                    log.error(e.getMessage(), e);
                }
            }

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo) {
        String reName = torrentsInfo.getName();

        if (StrUtil.isBlank(reName) || !ReUtil.contains(StringEnum.SEASON_REG, reName)) {
            Boolean start = start(torrentsInfo, config);
            Assert.isTrue(start, "开始任务失败 {}", reName);
            if (start) {
                log.info("开始任务 {}", reName);
            }
            return;
        }

        String hash = torrentsInfo.getHash();

        String host = config.getDownloadToolHost();

        Optional<Ani> aniOpt = DownloadService.findAniByDownloadPath(torrentsInfo);

        if (aniOpt.isEmpty()) {
            log.error("未能获取番剧对象: {}", torrentsInfo.getName());
            return;
        }

        Ani ani = aniOpt.get();

        List<String> priorityKeywords = getPriorityKeywords(config, ani);

        List<FileEntity> files = files(torrentsInfo, true, config);

        if (!priorityKeywords.isEmpty()) {
            files = files.stream()
                    .sorted(Comparator.comparingInt(file -> {
                        String fileName = file.getName();
                        int minIndex = Integer.MAX_VALUE;
                        for (int i = 0; i < priorityKeywords.size(); i++) {
                            String priorityKeyword = priorityKeywords.get(i);
                            if (!fileName.contains(priorityKeyword)) {
                                continue;
                            }
                            minIndex = Math.min(minIndex, i);
                        }
                        return minIndex;
                    }))
                    .toList();
        }

        List<String> names = files.stream()
                .map(FileEntity::getName)
                .toList();

        Assert.notEmpty(files, "{} 磁力链接还在获取原数据中", hash);

        List<String> newNames = new ArrayList<>();

        for (FileEntity fileEntity : files) {
            String name = fileEntity.getName();
            String newPath = getFileReName(name, reName);

            if (names.contains(newPath)) {
                continue;
            }
            if (newNames.contains(newPath)) {
                // 停止不必要的文件下载
                HttpReq.post(host + "/api/v2/torrents/filePrio")
                        .form("hash", hash)
                        .form("id", fileEntity.getIndex())
                        .form("priority", 0)
                        .thenFunction(HttpResponse::isOk);
                continue;
            }
            newNames.add(newPath);

            // 文件名未发生改变
            if (name.equals(newPath)) {
                continue;
            }

            log.info("重命名 {} ==> {}", name, newPath);

            Boolean b = HttpReq.post(host + "/api/v2/torrents/renameFile")
                    .form("hash", hash)
                    .form("oldPath", name)
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
            Assert.isTrue(b, "重命名失败 {} ==> {}", name, newPath);
        }

        Boolean start = start(torrentsInfo, config);
        Assert.isTrue(start, "开始任务失败 {}", reName);
        log.info("开始任务 {}", reName);

        if (newNames.isEmpty()) {
            return;
        }

        // qb重命名具有延迟，等待重命名完成
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(1000);
            names = torrentsInfo.getFiles().get();
            if (new HashSet<>(names).containsAll(newNames)) {
                return;
            }
        }
        log.warn("重命名貌似出现了问题？{}", reName);
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        String host = config.getDownloadToolHost();
        String hash = torrentsInfo.getHash();
        return HttpReq.post(host + "/api/v2/torrents/addTags")
                .form("hashes", hash)
                .form("tags", tags)
                .thenFunction(res -> {
                    boolean ok = res.isOk();
                    if (!ok) {
                        log.error(res.body());
                    }
                    return ok;
                });
    }

    @Override
    public void updateTrackers(Set<String> trackers) {
        String host = config.getDownloadToolHost();
        JsonObject preferences = HttpReq.get(host + "/api/v2/app/preferences")
                .thenFunction(res -> {
                    int status = res.getStatus();
                    boolean ok = res.isOk();
                    Assert.isTrue(ok, "更新trackers失败 {}", status);
                    String body = res.body();
                    return GsonStatic.fromJson(body, JsonObject.class);
                });

        preferences.addProperty("add_trackers", CollUtil.join(trackers, "\n"));
        preferences.addProperty("add_trackers_enabled", true);

        HttpReq.post(host + "/api/v2/app/setPreferences")
                .form("json", GsonStatic.toJson(preferences))
                .then(res -> {
                    if (res.isOk()) {
                        log.info("qBittorrent 更新Trackers完成 共{}条", trackers.size());
                        return;
                    }
                    log.error("qBittorrent 更新Trackers失败 {}", res.getStatus());
                });

    }

    @Override
    public void setSavePath(TorrentsInfo torrentsInfo, String path) {
        String host = config.getDownloadToolHost();
        HttpReq.post(host + "/api/v2/torrents/setAutoManagement")
                .form("hashes", torrentsInfo.getHash())
                .form("enable", false)
                .thenFunction(HttpResponse::isOk);
        HttpReq.post(host + "/api/v2/torrents/setSavePath")
                .form("id", torrentsInfo.getHash())
                .form("path", path)
                .then(req -> {
                    if (!req.isOk()) {
                        log.error(req.body());
                    }
                });
    }


    @Data
    @Accessors(chain = true)
    public static class FileEntity {
        private Integer index;
        private String name;
        private Long size;
        /**
         * 1 允许下载。2 禁止下载
         */
        private Integer priority;
    }

    private static List<String> getPriorityKeywords(Config config, Ani ani) {
        Boolean priorityKeywordsEnable = config.getPriorityKeywordsEnable();
        Boolean customPriorityKeywordsEnable = ani.getCustomPriorityKeywordsEnable();

        if (customPriorityKeywordsEnable) {
            return ani.getCustomPriorityKeywords();
        }

        if (priorityKeywordsEnable) {
            return config.getPriorityKeywords();
        }

        return new ArrayList<>();
    }


}
