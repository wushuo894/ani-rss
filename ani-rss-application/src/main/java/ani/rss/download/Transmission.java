package ani.rss.download;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.entity.torrent.TransmissionRpcBody;
import ani.rss.entity.torrent.TransmissionTorrentsInfo;
import ani.rss.entity.web.Header;
import ani.rss.enums.TorrentsTagEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.basic.RenameCacheUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Transmission
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Transmission implements BaseDownload {
    private static final Config CONFIG = ConfigUtil.CONFIG;
    private static String SESSION_ID = "";

    @Override
    public Boolean login(Boolean test, Config config) {
        String username = config.getDownloadToolUsername();
        String password = config.getDownloadToolPassword();
        String downloadToolHost = config.getDownloadToolHost();

        if (StrUtil.isBlank(downloadToolHost) ||
                StrUtil.isBlank(username) ||
                StrUtil.isBlank(password)
        ) {
            log.warn("Transmission 未配置完成");
            return false;
        }

        String authorization = StrFormatter.format("Basic {}", Base64.encode(username + ":" + password));
        Boolean isOk = HttpReq.get(downloadToolHost)
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
        try {
            TransmissionRpcBody transmissionRpcBody = TransmissionRpcBody.torrentGet();
            return rpc(transmissionRpcBody)
                    .thenFunction(res -> {
                        String sessionId = res.header("X-Transmission-Session-Id");
                        if (StrUtil.isNotBlank(sessionId)) {
                            SESSION_ID = sessionId;
                            return getTorrentsInfos();
                        }
                        TransmissionTorrentsInfo transmissionTorrentsInfo = GsonStatic.fromJson(res.body(), TransmissionTorrentsInfo.class);

                        List<TransmissionTorrentsInfo.Torrent> torrentsInfoList = transmissionTorrentsInfo
                                .getArguments()
                                .getTorrents();
                        return torrentsInfoList
                                .stream()
                                .map(TransmissionTorrentsInfo.Torrent::toTorrentsInfo)
                                .filter(torrentsInfo -> {
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

    public TransmissionRpcBody getTorrentAddBody(Ani ani, Item item, String savePath, File torrentFile) {
        String extName = FileUtil.extName(torrentFile);
        List<String> tags = newTags(ani, item);

        if ("txt".equals(extName)) {
            String magnet = FileUtil.readUtf8String(torrentFile);
            return TransmissionRpcBody.torrentAdd(tags, magnet, savePath);
        }
        if (torrentFile.length() > 0) {
            return TransmissionRpcBody.torrentAdd(tags, torrentFile, savePath);
        }
        String magnet = "magnet:?xt=urn:btih:" + FileUtil.mainName(torrentFile);
        return TransmissionRpcBody.torrentAdd(tags, magnet, savePath);
    }

    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile) {
        String name = item.getReName();

        TransmissionRpcBody transmissionRpcBody = getTorrentAddBody(ani, item, savePath, torrentFile);

        String id = rpc(transmissionRpcBody)
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return jsonObject.getAsJsonObject("arguments")
                            .getAsJsonObject("torrent-added")
                            .get("id").getAsString();
                });

        log.info("tr 添加下载 => name: {} id: {}", name, id);

        Boolean ova = ani.getOva();
        if (!ova) {
            RenameCacheUtil.put(id, name);
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
        String id = torrentsInfo.getId();
        TransmissionRpcBody transmissionRpcBody = TransmissionRpcBody.torrentRemove(id, deleteFiles);
        try {
            return rpc(transmissionRpcBody)
                    .thenFunction(HttpResponse::isOk);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String name = torrentsInfo.getName();

        if (ReUtil.contains("^\\w{40}$", name)) {
            log.debug("{} 磁力链接还在获取原数据中", name);
            return false;
        }

        String reName = RenameCacheUtil.get(id);
        if (StrUtil.isBlank(reName)) {
            log.debug("未获取到重命名 => id: {}", id);
            return false;
        }

        String extName = FileUtil.extName(name);
        if (StrUtil.isNotBlank(extName)) {
            reName = reName + "." + extName;
        }

        TransmissionRpcBody transmissionRpcBody = TransmissionRpcBody.torrentRenamePath(id, name, reName);

        log.info("重命名 {} ==> {}", name, reName);

        Boolean ok = rpc(transmissionRpcBody)
                .thenFunction(HttpResponse::isOk);
        Assert.isTrue(ok, "重命名失败 {} ==> {}", name, reName);
        RenameCacheUtil.remove(id);

        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(1000);
            Optional<TorrentsInfo> first = getTorrentsInfos()
                    .stream()
                    .filter(info -> info.getId().equals(id))
                    .findFirst();
            if (first.isEmpty()) {
                break;
            }
            if (first.get().getName().equals(reName)) {
                return true;
            }
        }

        log.warn("重命名貌似出现了问题？{}", reName);
        return false;
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tag) {
        String id = torrentsInfo.getId();
        List<String> tags = new ArrayList<>(torrentsInfo.getTagList());
        tags.add(tag);

        TransmissionRpcBody transmissionRpcBody = TransmissionRpcBody.torrentSet(id, tags);
        return rpc(transmissionRpcBody)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public void updateTrackers(Set<String> trackers) {
        log.info("Transmission暂时还不支持 自动更新Trackers");
    }

    @Override
    public void setSavePath(TorrentsInfo torrentsInfo, String path) {
        String id = torrentsInfo.getId();
        TransmissionRpcBody transmissionRpcBody = TransmissionRpcBody.torrentSetLocation(id, path);
        rpc(transmissionRpcBody)
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * rpc请求
     *
     * @param transmissionRpcBody 请求体
     * @return HttpRequest
     */
    private HttpRequest rpc(TransmissionRpcBody transmissionRpcBody) {
        String downloadToolHost = CONFIG.getDownloadToolHost();
        String username = CONFIG.getDownloadToolUsername();
        String password = CONFIG.getDownloadToolPassword();
        String authorization = StrFormatter.format("Basic {}", Base64.encode(username + ":" + password));

        return HttpReq.post(downloadToolHost + "/transmission/rpc")
                .header(Header.AUTHORIZATION, authorization)
                .header("X-Transmission-Session-Id", SESSION_ID)
                .body(GsonStatic.toJson(transmissionRpcBody));
    }
}
