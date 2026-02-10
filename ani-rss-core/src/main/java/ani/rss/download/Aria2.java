package ani.rss.download;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.basic.RenameCacheUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

/**
 * Aria2
 */
@Slf4j
public class Aria2 implements BaseDownload {
    private Config config;

    @Override
    public Boolean login(Boolean test, Config config) {
        this.config = config;
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(password)) {
            log.warn("Aria2 未配置完成");
            return false;
        }

        String body = ResourceUtil.readUtf8Str("aria2/getGlobalStat.json");
        body = StrFormatter.format(body, password);
        return HttpReq.post(host + "/jsonrpc")
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
        ThreadUtil.sleep(1000);
        try {
            torrentsInfos.addAll(getTorrentsInfos("aria2/tellActive.json"));
            torrentsInfos.addAll(getTorrentsInfos("aria2/tellWaiting.json"));
            torrentsInfos.addAll(getTorrentsInfos("aria2/tellStopped.json"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return torrentsInfos;
    }

    public List<TorrentsInfo> getTorrentsInfos(String type) {
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        String body = ResourceUtil.readUtf8Str(type);
        body = StrFormatter.format(body, password);
        return HttpReq.post(host + "/jsonrpc")
                .body(body)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    List<JsonElement> result = jsonObject.get("result").getAsJsonArray().asList();
                    List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                    for (JsonElement jsonElement : result) {
                        JsonObject asJsonObject = jsonElement.getAsJsonObject();
                        JsonElement bittorrent = asJsonObject.get("bittorrent");
                        if (Objects.isNull(bittorrent) || bittorrent.isJsonNull()) {
                            continue;
                        }
                        JsonElement info = bittorrent.getAsJsonObject()
                                .get("info");
                        if (Objects.isNull(info)) {
                            continue;
                        }
                        String name = info.getAsJsonObject()
                                .get("name").getAsString();
                        String infoHash = asJsonObject.get("infoHash").getAsString();
                        String status = asJsonObject.get("status").getAsString();
                        TorrentsInfo.State state = "complete".equals(status) ?
                                TorrentsInfo.State.pausedUP : TorrentsInfo.State.downloading;
                        String dir = asJsonObject.get("dir").getAsString();
                        String gid = asJsonObject.get("gid").getAsString();

                        List<String> files = asJsonObject.get("files")
                                .getAsJsonArray()
                                .asList()
                                .stream().map(JsonElement::getAsJsonObject)
                                .map(o -> o.get("path").getAsString())
                                .toList();

                        long size = asJsonObject.get("totalLength").getAsLong();
                        long completed = asJsonObject.get("completedLength").getAsLong();

                        TorrentsInfo torrentsInfo = new TorrentsInfo();
                        torrentsInfo
                                .progress(completed, size)
                                .setTags(List.of())
                                .setId(gid)
                                .setName(name)
                                .setHash(infoHash)
                                .setState(state)
                                .setDownloadDir(FileUtils.getAbsolutePath(dir))
                                .setFiles(() -> files);
                        torrentsInfos.add(torrentsInfo);
                    }
                    return torrentsInfos;
                });
    }


    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile, Boolean ova) {
        String name = item.getReName();
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        String body;

        String extName = FileUtil.extName(torrentFile);
        if (StrUtil.isBlank(extName)) {
            return false;
        }

        if ("txt".equals(extName)) {
            log.error("Aria2 暂不支持磁力链接下载与重命名");
            return false;
        } else {
            body = ResourceUtil.readUtf8Str("aria2/addTorrent.json");
            body = StrFormatter.format(body, password, Base64.encode(torrentFile), savePath);
        }

        String id = HttpReq.post(host + "/jsonrpc")
                .body(body)
                .thenFunction(res -> GsonStatic.fromJson(res.body(), JsonObject.class).get("result").getAsString());

        log.info("aria2 添加下载 => name: {} id: {}", name, id);

        Boolean watchErrorTorrent = config.getWatchErrorTorrent();

        if (!ova) {
            RenameCacheUtil.put(id, name);
        }

        if (!watchErrorTorrent) {
            ThreadUtil.sleep(1000 * 10);
            return true;
        }

        for (int i = 0; i < 3; i++) {
            ThreadUtil.sleep(1000 * 10);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                if (!torrentsInfo.getId().equals(id)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles) {
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        String id = torrentsInfo.getId();
        String body = ResourceUtil.readUtf8Str("aria2/removeDownloadResult.json");
        body = StrFormatter.format(body, password, id);

        try {
            return HttpReq.post(host + "/jsonrpc")
                    .body(body)
                    .thenFunction(HttpResponse::isOk);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String downloadDir = torrentsInfo.getDownloadDir();
        TorrentsInfo.State state = torrentsInfo.getState();

        if (Objects.isNull(state)) {
            return false;
        }

        // 仅支持下载完成后重命名
        if (!state.name().equals(TorrentsInfo.State.pausedUP.name())) {
            return false;
        }

        String reName = RenameCacheUtil.get(id);
        if (StrUtil.isBlank(reName)) {
            log.debug("未获取到重命名 => id: {}", id);
            return false;
        }

        List<File> files = torrentsInfo.getFiles().get()
                .stream()
                .map(File::new)
                .filter(File::exists)
                .filter(file -> {
                    String extName = FileUtil.extName(file);
                    if (StrUtil.isBlank(extName)) {
                        return false;
                    }
                    if (file.length() < 1) {
                        return false;
                    }
                    return FileUtils.isVideoFormat(extName) || FileUtils.isSubtitleFormat(extName);
                })
                .sorted(Comparator.comparingLong(file -> Long.MAX_VALUE - file.length()))
                .toList();

        Assert.notEmpty(files, "映射路径存在错误, 无法重命名");

        for (File src : files) {
            String name = src.getName();
            String fileReName = getFileReName(name, reName);
            File newPath = new File(downloadDir + "/" + fileReName);
            if (FileUtil.equals(src, newPath)) {
                continue;
            }
            FileUtil.move(src, newPath, false);
            log.info("重命名 {} ==> {}", name, newPath);
        }
        RenameCacheUtil.remove(id);

        return true;
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        return false;
    }

    @Override
    public void updateTrackers(Set<String> trackers) {
        String trackersStr = CollUtil.join(trackers, "\\n");
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        String body = ResourceUtil.readUtf8Str("aria2/changeGlobalOption.json");
        body = StrFormatter.format(body, password, trackersStr);

        HttpReq.post(host + "/jsonrpc")
                .body(body)
                .then(res -> {
                    if (res.isOk()) {
                        log.info("Aria2 更新Trackers完成 共{}条", trackers.size());
                        return;
                    }
                    log.error("Aria2 更新Trackers失败 {}", res.getStatus());
                });
    }

    @Override
    public void setSavePath(TorrentsInfo torrentsInfo, String path) {
        // api 不支持
    }
}
