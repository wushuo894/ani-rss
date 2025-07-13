package ani.rss.download;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.TorrentsTags;
import ani.rss.util.FilePathUtil;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import ani.rss.util.RenameCacheUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Transmission
 */
@Slf4j
public class Transmission implements BaseDownload {
    private String host = "";
    private String authorization = "";
    private String sessionId = "";
    private Config config;

    @Override
    public Boolean login(Boolean test, Config config) {
        this.config = config;
        String username = config.getDownloadToolUsername();
        String password = config.getDownloadToolPassword();
        host = config.getDownloadToolHost();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(username)
                || StrUtil.isBlank(password)) {
            log.warn("Transmission 未配置完成");
            return false;
        }

        authorization = StrFormatter.format("Basic {}", Base64.encode(username + ":" + password));
        Boolean isOk = HttpReq.get(host, false)
                .header(Header.AUTHORIZATION, authorization)
                .thenFunction(HttpResponse::isOk);
        if (!isOk) {
            log.error("登录 Transmission 失败");
            return false;
        }
        getTorrentsInfos();
        return true;
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        String body = ResourceUtil.readUtf8Str("transmission/torrent-get.json");
        try {
            return HttpReq.post(host + "/transmission/rpc", false)
                    .header(Header.AUTHORIZATION, authorization)
                    .header("X-Transmission-Session-Id", sessionId)
                    .body(body)
                    .thenFunction(res -> {
                        String id = res.header("X-Transmission-Session-Id");
                        if (StrUtil.isNotBlank(id)) {
                            sessionId = id;
                            return getTorrentsInfos();
                        }
                        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        JsonArray torrents = jsonObject.get("arguments")
                                .getAsJsonObject()
                                .get("torrents")
                                .getAsJsonArray();
                        for (JsonElement jsonElement : torrents.asList()) {
                            JsonObject item = jsonElement.getAsJsonObject();
                            List<String> tags = item.get("labels").getAsJsonArray()
                                    .asList().stream().map(JsonElement::getAsString)
                                    .toList();
                            if (!tags.contains(TorrentsTags.ANI_RSS.getValue())) {
                                continue;
                            }
                            List<String> files = item.get("files").getAsJsonArray().asList()
                                    .stream().map(JsonElement::getAsJsonObject)
                                    .map(o -> o.get("name").getAsString())
                                    .toList();

                            // 状态： https://github.com/jayzcoder/TrguiNG/blob/zh/src/rpc/transmission.ts

                            TorrentsInfo.State state = TorrentsInfo.State.downloading;

                            // 做种中
                            if (item.get("status").getAsInt() == 6) {
                                state = TorrentsInfo.State.stalledUP;
                            }

                            // 已完成
                            if (item.get("isFinished").getAsBoolean()) {
                                state = TorrentsInfo.State.pausedUP;
                            }

                            String downloadDir = item.get("downloadDir").getAsString();
                            long size = item.get("totalSize").getAsLong();
                            long completed = item.get("haveValid").getAsLong();

                            TorrentsInfo torrentsInfo = new TorrentsInfo();
                            torrentsInfo.progress(completed, size)
                                    .setName(item.get("name").getAsString())
                                    .setTags(tags)
                                    .setHash(item.get("hashString").getAsString())
                                    .setState(state)
                                    .setId(item.get("id").getAsString())
                                    .setDownloadDir(FilePathUtil.getAbsolutePath(downloadDir))
                                    .setFiles(() -> files);
                            torrentsInfos.add(torrentsInfo);
                        }
                        return torrentsInfos;
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile, Boolean ova) {
        String name = item.getReName();
        Boolean master = item.getMaster();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
        String body = ResourceUtil.readUtf8Str("transmission/torrent-add.json");
        String extName = FileUtil.extName(torrentFile);
        if (StrUtil.isBlank(extName)) {
            return false;
        }

        List<String> tags = new ArrayList<>();
        tags.add(TorrentsTags.ANI_RSS.getValue());
        tags.add(subgroup);
        if (!master) {
            tags.add(TorrentsTags.BACK_RSS.getValue());
        }

        String torrent;
        if ("txt".equals(extName)) {
            torrent = FileUtil.readUtf8String(torrentFile);
            body = StrFormatter.format(body, GsonStatic.toJson(tags), savePath, "", torrent);
        } else {
            if (torrentFile.length() > 0) {
                torrent = Base64.encode(torrentFile);
                body = StrFormatter.format(body, GsonStatic.toJson(tags), savePath, torrent, "");
            } else {
                torrent = "magnet:?xt=urn:btih:" + FileUtil.mainName(torrentFile);
                body = StrFormatter.format(body, GsonStatic.toJson(tags), savePath, "", torrent);
            }
        }

        String id = HttpReq.post(host + "/transmission/rpc", false)
                .timeout(1000 * 60)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return jsonObject.getAsJsonObject("arguments")
                            .getAsJsonObject("torrent-added")
                            .get("id").getAsString();
                });

        log.info("tr 添加下载 => name: {} id: {}", name, id);

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
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos
                    .stream()
                    .filter(torrentsInfo -> torrentsInfo.getId().equals(id))
                    .findFirst();
            if (optionalTorrentsInfo.isEmpty()) {
                continue;
            }
            return true;
        }

        return false;
    }

    @Override
    public Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles) {
        String body = ResourceUtil.readUtf8Str("transmission/torrent-remove.json");
        body = StrFormatter.format(body, torrentsInfo.getId(), deleteFiles);
        try {
            return HttpReq.post(host + "/transmission/rpc", false)
                    .header(Header.AUTHORIZATION, authorization)
                    .header("X-Transmission-Session-Id", sessionId)
                    .body(body)
                    .thenFunction(HttpResponse::isOk);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String name = torrentsInfo.getName();

        Assert.isTrue(!ReUtil.contains("^\\w{40}$", name), "{} 磁力链接还在获取原数据中", name);

        String reName = RenameCacheUtil.get(id);
        if (StrUtil.isBlank(reName)) {
            log.debug("未获取到重命名 => id: {}", id);
            return;
        }

        String extName = FileUtil.extName(name);
        if (StrUtil.isNotBlank(extName)) {
            reName = reName + "." + extName;
        }

        String body = ResourceUtil.readUtf8Str("transmission/torrent-rename-path.json");
        body = StrFormatter.format(body, id, name, reName);

        log.info("重命名 {} ==> {}", name, reName);

        Boolean ok = HttpReq.post(host + "/transmission/rpc", false)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .thenFunction(HttpResponse::isOk);
        Assert.isTrue(ok, "重命名失败 {} ==> {}", name, reName);
        RenameCacheUtil.remove(id);

        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(1000);
            Optional<TorrentsInfo> first = getTorrentsInfos().stream()
                    .filter(info -> info.getId().equals(id))
                    .findFirst();
            if (first.isEmpty()) {
                break;
            }
            if (first.get().getName().equals(reName)) {
                return;
            }
        }
        log.warn("重命名貌似出现了问题？{}", reName);
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tag) {
        String id = torrentsInfo.getId();
        List<String> tags = torrentsInfo.getTags();
        List<String> strings = new ArrayList<>(tags);
        strings.add(tag);

        String body = ResourceUtil.readUtf8Str("transmission/torrent-set.json");
        body = StrFormatter.format(body, GsonStatic.toJson(strings), id);
        return HttpReq.post(host + "/transmission/rpc", false)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public void updateTrackers(Set<String> trackers) {
        log.info("Transmission暂时还不支持 自动更新Trackers");
    }

    @Override
    public void setSavePath(TorrentsInfo torrentsInfo, String path) {
        String id = torrentsInfo.getId();
        String body = ResourceUtil.readUtf8Str("transmission/torrent-set-location.json");
        body = StrFormatter.format(body, id, path);
        HttpReq.post(host + "/transmission/rpc", false)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }
}
