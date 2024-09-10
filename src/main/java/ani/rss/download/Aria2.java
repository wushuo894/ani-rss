package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Aria2 implements BaseDownload {
    @Override
    public Boolean login() {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String password = config.getPassword();
        String body = ResourceUtil.readUtf8Str("aria2/getGlobalStat.json");
        body = StrFormatter.format(body, password);
        return HttpReq.post(host + "/jsonrpc", false)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
        ThreadUtil.sleep(1000);
        torrentsInfos.addAll(getTorrentsInfos("aria2/tellActive.json"));
        ThreadUtil.sleep(1000);
        torrentsInfos.addAll(getTorrentsInfos("aria2/tellStopped.json"));
        return torrentsInfos;
    }

    public List<TorrentsInfo> getTorrentsInfos(String type) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String password = config.getPassword();
        String body = ResourceUtil.readUtf8Str(type);
        body = StrFormatter.format(body, password);
        return HttpReq.post(host + "/jsonrpc", false)
                .body(body)
                .thenFunction(res -> {
                    JsonObject jsonObject = gson.fromJson(res.body(), JsonObject.class);
                    List<JsonElement> result = jsonObject.get("result").getAsJsonArray().asList();
                    List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                    for (JsonElement jsonElement : result) {
                        JsonObject asJsonObject = jsonElement.getAsJsonObject();
                        JsonElement bittorrent = asJsonObject.get("bittorrent");
                        if (bittorrent.isJsonNull()) {
                            continue;
                        }
                        String name = bittorrent.getAsJsonObject()
                                .get("info").getAsJsonObject()
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
                                .collect(Collectors.toList());

                        TorrentsInfo torrentsInfo = new TorrentsInfo();
                        torrentsInfo
                                .setId(gid)
                                .setName(name)
                                .setHash(infoHash)
                                .setState(state)
                                .setDownloadDir(dir)
                                .setFiles(files);
                        torrentsInfos.add(torrentsInfo);
                    }
                    return torrentsInfos;
                });
    }


    @Override
    public Boolean download(String name, String savePath, File torrentFile, Boolean ova) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String password = config.getPassword();
        Integer renameSleep = config.getRenameSleep();
        String body = ResourceUtil.readUtf8Str("aria2/addTorrent.json");
        body = StrFormatter.format(body, password, Base64.encode(torrentFile), savePath);

        String gid = HttpReq.post(host + "/jsonrpc", false)
                .body(body)
                .thenFunction(res -> gson.fromJson(res.body(), JsonObject.class).get("result").getAsString());
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(3000);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            for (TorrentsInfo torrentsInfo : torrentsInfos) {
                if (torrentsInfo.getId().equals(gid)) {
                    renameCache.put(gid, name, renameSleep * (1000 * 60) * 3);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String password = config.getPassword();
        String id = torrentsInfo.getId();
        String body = ResourceUtil.readUtf8Str("aria2/removeDownloadResult.json");
        body = StrFormatter.format(body, password, id);

        HttpReq.post(host + "/jsonrpc", false)
                .body(body)
                .thenFunction(HttpResponse::isOk);
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo) {
        String id = torrentsInfo.getId();
        String downloadDir = torrentsInfo.getDownloadDir();
        String reName = renameCache.get(id);
        List<String> files = torrentsInfo.getFiles();
        for (String file : files) {
            String name = new File(file).getName();
            File newPath = new File(downloadDir + "/" + name);
            String fileReName = getFileReName(name, reName);
            if (!name.equals(fileReName)) {
                newPath = new File(downloadDir + "/" + fileReName);
            }
            if (file.equals(newPath.getAbsolutePath())) {
                continue;
            }
            FileUtil.move(new File(file), newPath, false);
            log.info("重命名 {} ==> {}", name, newPath);
        }
    }
}
