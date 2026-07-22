package ani.rss.download;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.entity.torrent.qBittorrentTorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TorrentsTagEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * qBittorrent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class qBittorrent implements BaseDownload {

    private static final Config CONFIG = ConfigUtil.CONFIG;

    private final DownloadService downloadService;


    /**
     * 获取对应任务的文件列表
     *
     * @param torrentsInfo 种子信息
     * @param filter       过滤出视频与字幕
     * @param config       设置
     * @return 文件列表
     */
    public static List<qBittorrentTorrentsInfo.FileEntity> files(TorrentsInfo torrentsInfo, Boolean filter, Config config) {
        String hash = torrentsInfo.getHash();
        String host = config.getDownloadToolHost();

        return HttpReq.get(host + "/api/v2/torrents/files")
                .form("hash", hash)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJsonList(res.body(), qBittorrentTorrentsInfo.FileEntity.class).stream()
                            .filter(fileEntity -> {
                                if (!filter) {
                                    return true;
                                }
                                String name = fileEntity.getName();
                                String extName = FileUtil.extName(name);
                                if (StrUtil.isBlank(extName)) {
                                    return false;
                                }
                                extName = extName.toLowerCase();
                                Long size = fileEntity.getSize();
                                if (size < 1) {
                                    return false;
                                }
                                return FileUtils.isVideoFormat(extName) || FileUtils.isSubtitleFormat(extName);
                            })
                            .sorted((fileEntity1, fileEntity2) -> Long.compare(fileEntity2.getSize(), fileEntity1.getSize()))
                            .toList();
                });
    }

    @Override
    public Boolean login(Boolean test, Config config) {
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
                        if (StrUtil.isBlank(body)) {
                            // 在 5.2.0 后登录会响应空 body
                            return true;
                        }
                        return "Ok.".equalsIgnoreCase(body);
                    });
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            log.error("登录 qBittorrent 失败 {}", message);
        }
        return false;
    }

    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile) {
        String name = item.getReName();
        String host = CONFIG.getDownloadToolHost();
        Boolean qbUseDownloadPath = CONFIG.getQbUseDownloadPath();

        List<String> tags = newTags(ani, item);

        Integer ratioLimit = CONFIG.getRatioLimit();
        Integer seedingTimeLimit = CONFIG.getSeedingTimeLimit();
        Integer inactiveSeedingTimeLimit = CONFIG.getInactiveSeedingTimeLimit();
        Boolean rename = CONFIG.getRename();

        Long upLimit = CONFIG.getUpLimit() * 1024;
        Long dlLimit = CONFIG.getDlLimit() * 1024;

        HttpRequest httpRequest = HttpReq.post(host + "/api/v2/torrents/add")
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("category", TorrentsTagEnum.ANI_RSS.getValue())
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
     * @param torrentsInfo 种子信息
     * @param config       设置
     * @return 下载是否成功
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
        String host = CONFIG.getDownloadToolHost();
        try {
            return HttpReq.get(host + "/api/v2/torrents/info")
                    .thenFunction(res -> {
                        List<qBittorrentTorrentsInfo> torrentsInfos = GsonStatic.fromJsonList(res.body(), qBittorrentTorrentsInfo.class);
                        return torrentsInfos.stream()
                                .map(qBittorrentTorrentsInfo::toTorrentsInfo)
                                .filter(torrentsInfo -> {
                                    // 过滤出 ani-rss 标签或分类
                                    String category = torrentsInfo.getCategory();
                                    if (category.equals(TorrentsTagEnum.ANI_RSS.getValue())) {
                                        return true;
                                    }

                                    List<String> tagList = torrentsInfo.getTagList();
                                    return tagList.contains(TorrentsTagEnum.ANI_RSS.getValue());
                                })
                                .toList();
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles) {
        String host = CONFIG.getDownloadToolHost();
        String name = torrentsInfo.getName();
        String hash = torrentsInfo.getHash();
        try {
            List<qBittorrentTorrentsInfo.FileEntity> files = files(torrentsInfo, false, CONFIG);
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

            String downloadDir = torrentsInfo.getSavePath();

            List<File> dirList = files.stream()
                    .map(qBittorrentTorrentsInfo.FileEntity::getName)
                    .map(File::new)
                    .map(File::getParent)
                    .filter(StrUtil::isNotBlank)
                    .map(s -> downloadDir + "/" + s)
                    .distinct()
                    .map(File::new)
                    .filter(File::exists)
                    .filter(File::isDirectory)
                    .toList();

            Boolean subtitleIndependentFolderEnabled = CONFIG.getSubtitleIndependentFolderEnabled();
            String subtitleIndependentFolderName = CONFIG.getSubtitleIndependentFolderName();

            // 清空剩余文件夹
            for (File file : dirList) {
                if (subtitleIndependentFolderEnabled) {
                    if (subtitleIndependentFolderName.equals(file.getName())) {
                        // 字幕独立文件夹 不进行删除
                        continue;
                    }
                }

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
    public Boolean rename(TorrentsInfo torrentsInfo) {
        String reName = torrentsInfo.getName();

        if (StrUtil.isBlank(reName) || !ReUtil.contains(StringEnum.SEASON_REG, reName)) {
            // 剧场版 OR OVA 直接开始任务
            Boolean start = start(torrentsInfo, CONFIG);
            Assert.isTrue(start, "开始任务失败 {}", reName);
            if (start) {
                log.info("开始任务 {}", reName);
            }
            return true;
        }

        String hash = torrentsInfo.getHash();

        String host = CONFIG.getDownloadToolHost();

        Optional<Ani> aniOpt = downloadService.findAniByDownloadPath(torrentsInfo);

        if (aniOpt.isEmpty()) {
            log.error("未能获取番剧对象: {}", torrentsInfo.getName());
            return false;
        }

        Ani ani = aniOpt.get();

        List<String> priorityKeywords = getPriorityKeywords(CONFIG, ani);

        List<qBittorrentTorrentsInfo.FileEntity> files = files(torrentsInfo, true, CONFIG);

        if (!priorityKeywords.isEmpty()) {
            files = files.stream()
                    .sorted(Comparator.comparingInt(file -> {
                        String fileName = file.getName();
                        String mainName = FileUtil.mainName(fileName);
                        int minIndex = Integer.MAX_VALUE;
                        for (int i = 0; i < priorityKeywords.size(); i++) {
                            String priorityKeyword = priorityKeywords.get(i);
                            if (!mainName.contains(priorityKeyword)) {
                                continue;
                            }
                            minIndex = Math.min(minIndex, i);
                        }
                        return minIndex;
                    }))
                    .toList();
        }

        List<String> names = files.stream()
                .map(qBittorrentTorrentsInfo.FileEntity::getName)
                .toList();

        if (files.isEmpty()) {
            log.debug("{} 磁力链接还在获取原数据中", hash);
            return false;
        }

        Boolean subtitleIndependentFolderEnabled = CONFIG.getSubtitleIndependentFolderEnabled();
        String subtitleIndependentFolderName = CONFIG.getSubtitleIndependentFolderName();

        List<String> newNames = new ArrayList<>();

        for (qBittorrentTorrentsInfo.FileEntity fileEntity : files) {
            String name = fileEntity.getName();
            String newPath = getFileReName(name, reName);

            if (
                    FileUtils.isSubtitleFormat(newPath) &&
                            subtitleIndependentFolderEnabled &&
                            StrUtil.isNotBlank(subtitleIndependentFolderName)
            ) {
                // 字幕独立文件夹
                newPath = subtitleIndependentFolderName + "/" + newPath;
            }

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

        Boolean start = start(torrentsInfo, CONFIG);
        Assert.isTrue(start, "开始任务失败 {}", reName);
        log.info("开始任务 {}", reName);

        if (newNames.isEmpty()) {
            return true;
        }

        // qb重命名具有延迟，等待重命名完成
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(1000);
            names = torrentsInfo.getFilesSupplier().get();
            if (new HashSet<>(names).containsAll(newNames)) {
                return true;
            }
        }

        log.warn("重命名貌似出现了问题？{}", reName);
        return false;
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        String host = CONFIG.getDownloadToolHost();
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
        String host = CONFIG.getDownloadToolHost();
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
        String host = CONFIG.getDownloadToolHost();
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
