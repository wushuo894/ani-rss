package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
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
import java.util.stream.Collectors;

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
    public Boolean login(Config config) {
        this.config = config;
        String username = config.getUsername();
        String password = config.getPassword();
        host = config.getHost();

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
        try {
            getTorrentsInfos();
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            log.error("登录 Transmission 失败 {}", message);
            return false;
        }
        return true;
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        String body = ResourceUtil.readUtf8Str("transmission/torrent-get.json");

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
                    JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                    JsonArray torrents = jsonObject.get("arguments")
                            .getAsJsonObject()
                            .get("torrents")
                            .getAsJsonArray();
                    for (JsonElement jsonElement : torrents.asList()) {
                        JsonObject item = jsonElement.getAsJsonObject();
                        List<String> tags = item.get("labels").getAsJsonArray()
                                .asList().stream().map(JsonElement::getAsString).collect(Collectors.toList());
                        if (!tags.contains(tag)) {
                            continue;
                        }
                        List<String> files = item.get("files").getAsJsonArray().asList()
                                .stream().map(JsonElement::getAsJsonObject)
                                .map(o -> o.get("name").getAsString())
                                .collect(Collectors.toList());


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

                        TorrentsInfo torrentsInfo = new TorrentsInfo();
                        torrentsInfo.setName(item.get("name").getAsString());
                        torrentsInfo.setTags(CollUtil.join(tags, ","));
                        torrentsInfo.setHash(item.get("hashString").getAsString());
                        torrentsInfo.setState(state);
                        torrentsInfo.setId(item.get("id").getAsString());
                        torrentsInfo.setDownloadDir(item.get("downloadDir").getAsString());
                        torrentsInfo.setFiles(files);
                        torrentsInfos.add(torrentsInfo);
                    }
                    return torrentsInfos;
                });
    }

    @Override
    public Boolean download(Item item, String savePath, File torrentFile, Boolean ova) {
        String name = item.getReName();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");
        String body = ResourceUtil.readUtf8Str("transmission/torrent-add.json");
        String extName = FileUtil.extName(torrentFile);
        if (StrUtil.isBlank(extName)) {
            return false;
        }
        String torrent = "";
        if ("txt".equals(extName)) {
            torrent = FileUtil.readUtf8String(torrentFile);
            body = StrFormatter.format(body, tag, subgroup, savePath, "", torrent);
        } else {
            torrent = Base64.encode(torrentFile);
            body = StrFormatter.format(body, tag, subgroup, savePath, torrent, "");
        }

        String id = HttpReq.post(host + "/transmission/rpc", false)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .thenFunction(res -> {
                    JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                    return jsonObject.getAsJsonObject("arguments")
                            .getAsJsonObject("torrent-added")
                            .get("id").getAsString();
                });

        Boolean watchErrorTorrent = config.getWatchErrorTorrent();

        if (!watchErrorTorrent) {
            if (!ova) {
                renameCache.put(id, name);
            }
            return true;
        }

        List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(2000);
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos
                    .stream()
                    .filter(torrentsInfo -> torrentsInfo.getId().equals(id))
                    .findFirst();
            if (optionalTorrentsInfo.isEmpty()) {
                continue;
            }
            if (!ova) {
                renameCache.put(id, name);
            }
            return true;
        }

        return false;
    }

    @Override
    public void delete(TorrentsInfo torrentsInfo) {
        String body = ResourceUtil.readUtf8Str("transmission/torrent-remove.json");
        body = StrFormatter.format(body, torrentsInfo.getId());
        HttpReq.post(host + "/transmission/rpc", false)
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", sessionId)
                .body(body)
                .then(HttpResponse::isOk);
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String name = torrentsInfo.getName();

        Assert.isTrue(!ReUtil.contains("^\\w{40}$", name), "{} 磁力链接还在获取原数据中", name);

        String mainName = FileUtil.mainName(name);

        if (ReUtil.contains(StringEnum.SEASON_REG, mainName)) {
            return;
        }

        String reName = renameCache.get(id);
        if (StrUtil.isBlank(reName)) {
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
        renameCache.remove(id);
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        String id = torrentsInfo.getId();
        List<String> strings = new ArrayList<>(List.of(torrentsInfo.getTags().split(",")));
        strings.add(tags);

        String body = ResourceUtil.readUtf8Str("transmission/torrent-set.json");
        body = StrFormatter.format(body, gson.toJson(strings), id);
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
}
