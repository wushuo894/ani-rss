package ani.rss.download;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.torrent.Aria2RpcBody;
import ani.rss.entity.torrent.Aria2TorrentsInfo;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.enums.TorrentsStateEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.basic.RenameCacheUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
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
 * Aria2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Aria2 implements BaseDownload {

    @Override
    public Boolean login(Boolean test, Config config) {
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(password)) {
            log.warn("Aria2 未配置完成");
            return false;
        }

        Aria2RpcBody aria2RpcBody = Aria2RpcBody.getGlobalStat();

        List<Object> params = aria2RpcBody.getParams();
        params.remove(0);
        params.add("token:" + password);

        return rpc(aria2RpcBody)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
        ThreadUtil.sleep(1000);
        try {
            torrentsInfos.addAll(getTorrentsInfos(Aria2RpcBody.tellActive()));
            torrentsInfos.addAll(getTorrentsInfos(Aria2RpcBody.tellWaiting()));
            torrentsInfos.addAll(getTorrentsInfos(Aria2RpcBody.tellStopped()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return torrentsInfos;
    }

    public List<TorrentsInfo> getTorrentsInfos(Aria2RpcBody aria2RpcBody) {
        return rpc(aria2RpcBody)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    Aria2TorrentsInfo aria2TorrentsInfo = GsonStatic.fromJson(res.body(), Aria2TorrentsInfo.class);

                    List<Aria2TorrentsInfo.Torrent> result = aria2TorrentsInfo.getResult();

                    return result
                            .stream()
                            .filter(torrent -> {
                                Aria2TorrentsInfo.Bittorrent bittorrent = torrent.getBittorrent();
                                if (Objects.isNull(bittorrent)) {
                                    return false;
                                }
                                Aria2TorrentsInfo.Bittorrent.Info info = bittorrent.getInfo();
                                if (Objects.isNull(info)) {
                                    return false;
                                }
                                String name = info.getName();
                                return StrUtil.isNotBlank(name);
                            })
                            .map(Aria2TorrentsInfo.Torrent::toTorrentsInfo)
                            .toList();
                });
    }

    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile) {
        String name = item.getReName();

        String extName = FileUtil.extName(torrentFile);
        if (StrUtil.isBlank(extName)) {
            return false;
        }

        if ("txt".equals(extName)) {
            log.error("Aria2 暂不支持磁力链接下载与重命名");
            return false;
        }

        Aria2RpcBody aria2RpcBody = Aria2RpcBody.addTorrent(torrentFile, savePath);

        String id = rpc(aria2RpcBody)
                .thenFunction(res ->
                        GsonStatic.fromJson(res.body(), JsonObject.class)
                                .get("result").getAsString()
                );

        log.info("aria2 添加下载 => name: {} id: {}", name, id);

        Boolean ova = ani.getOva();
        if (!ova) {
            RenameCacheUtil.put(id, name);
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
        String id = torrentsInfo.getId();

        Aria2RpcBody aria2RpcBody = Aria2RpcBody.removeDownloadResult(id);

        try {
            return rpc(aria2RpcBody)
                    .thenFunction(HttpResponse::isOk);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String savePath = torrentsInfo.getSavePath();
        TorrentsStateEnum torrentsState = torrentsInfo.getState();

        // 仅支持下载完成后重命名
        if (torrentsState != TorrentsStateEnum.stoppedUP) {
            return false;
        }

        String reName = RenameCacheUtil.get(id);
        if (StrUtil.isBlank(reName)) {
            log.debug("未获取到重命名 => id: {}", id);
            return false;
        }

        List<File> files = torrentsInfo.getFilesSupplier().get()
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
            File newPath = new File(savePath, fileReName);
            if (FileUtil.equals(src, newPath)) {
                continue;
            }
            FileUtil.move(src, newPath, true);
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
        String trackersStr = CollUtil.join(trackers, ", ");

        Aria2RpcBody aria2RpcBody = Aria2RpcBody.changeGlobalOption(trackersStr);

        rpc(aria2RpcBody)
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

    /**
     * rpc请求
     *
     * @param aria2RpcBody 请求体
     * @return HttpRequest
     */
    private HttpRequest rpc(Aria2RpcBody aria2RpcBody) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getDownloadToolHost();
        return HttpReq.post(host + "/jsonrpc")
                .body(GsonStatic.toJson(aria2RpcBody));
    }
}