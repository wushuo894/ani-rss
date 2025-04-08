package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.download.qBittorrent;
import ani.rss.entity.*;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import ani.rss.util.RenameUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.bittorrent.TorrentFile;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Auth
@Path("/collection")
public class CollectionAction implements BaseAction {
    public static synchronized List<Item> preview(CollectionInfo collectionInfo) {
        String torrent = collectionInfo.getTorrent();
        File tempFile = FileUtil.createTempFile();
        Base64.decodeToFile(torrent, tempFile);
        TorrentFile torrentFile;
        try {
            torrentFile = new TorrentFile(tempFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Ani ani = collectionInfo.getAni();
        long[] lengths = torrentFile.getLengths();
        AtomicInteger index = new AtomicInteger(0);

        return Arrays.stream(torrentFile.getFilenames())
                .map(FileUtil::getName)
                .map(fileName -> {
                    long length = lengths[index.getAndIncrement()];

                    Double l = length / 1024.0 / 1024;

                    String size = NumberUtil.decimalFormat("0.00", l) + "MB";

                    Item item = new Item()
                            .setTitle(fileName)
                            .setSize(size);

                    RenameUtil.rename(ani, item, new HashMap<>());

                    String reName = item.getReName();

                    reName = reName + "." + FileUtil.extName(fileName);

                    return item.setReName(reName)
                            .setLength(length);
                })
                .toList();
    }

    public static synchronized void download(String name, File torrentFile, String savePath, List<String> tags) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String download = config.getDownload();
        Assert.isTrue("qBittorrent".equals(download), "合集下载暂时只支持 qBittorrent");

        Assert.isTrue(TorrentUtil.login(), "下载器登录失败");

        Integer ratioLimit = config.getRatioLimit();
        Integer seedingTimeLimit = config.getSeedingTimeLimit();
        Integer inactiveSeedingTimeLimit = config.getInactiveSeedingTimeLimit();

        Long upLimit = config.getUpLimit() * 1024;
        Long dlLimit = config.getDlLimit() * 1024;

        Boolean qbUseDownloadPath = config.getQbUseDownloadPath();

        HttpReq.post(host + "/api/v2/torrents/add", false)
                .form("torrents", torrentFile)
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("category", "")
                .form("contentLayout", "Original")
                .form("dlLimit", dlLimit)
                .form("firstLastPiecePrio", false)
                .form("paused", false)
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
                .form("inactiveSeedingTimeLimit", inactiveSeedingTimeLimit)
                .then(res -> Assert.isTrue(res.isOk(), "status: {}", res.getStatus()));
    }

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) {
        String type = request.getParam("type");

        CollectionInfo collectionInfo = getBody(CollectionInfo.class);
        String torrent = collectionInfo.getTorrent();

        if (type.equals("preview")) {
            resultSuccess(preview(collectionInfo));
        }

        if (!type.equals("start")) {
            return;
        }

        File tempFile = FileUtil.createTempFile();
        Base64.decodeToFile(torrent, tempFile);
        TorrentFile torrentFile;
        try {
            torrentFile = new TorrentFile(tempFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Ani ani = collectionInfo.getAni();
        String title = ani.getTitle();
        String subgroup = ani.getSubgroup();
        String downloadPath = ani.getDownloadPath();

        String name = StrFormatter.format("[ANI-RSS合集下载] {}", title);
        download(name, tempFile, downloadPath, List.of("ANI-RSS合集下载", subgroup));

        TorrentsInfo torrentsInfo = new TorrentsInfo()
                .setHash(torrentFile.getHexHash());

        Config config = ConfigUtil.CONFIG;

        MD5 md5 = MD5.create();

        Map<String, String> reNameMap = preview(collectionInfo)
                .stream()
                .collect(Collectors.toMap(
                        item -> md5.digestHex(item.getTitle() + item.getLength()),
                        Item::getReName)
                );

        String host = config.getHost();

        List<qBittorrent.FileEntity> files = qBittorrent.files(torrentsInfo, config);
        for (qBittorrent.FileEntity file : files) {
            name = new File(file.getName()).getName();
            String key = md5.digestHex(name + file.getSize());
            if (!reNameMap.containsKey(key)) {
                continue;
            }
            String newPath = reNameMap.get(key);

            log.info("重命名 {} ==> {}", name, newPath);
            Boolean b = HttpReq.post(host + "/api/v2/torrents/renameFile", false)
                    .form("hash", torrentFile.getHexHash())
                    .form("oldPath", file.getName())
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
            if (!b) {
                log.error("重命名失败 {} ==> {}", name, newPath);
            }
        }
    }


}
