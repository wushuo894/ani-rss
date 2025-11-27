package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.download.BaseDownload;
import ani.rss.download.qBittorrent;
import ani.rss.entity.*;
import ani.rss.enums.StringEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AfdianUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.RenameUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.bittorrent.TorrentFile;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 合集
 */
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

        List<String> match = ani.getMatch();
        List<String> exclude = ani.getExclude();
        Boolean globalExclude = ani.getGlobalExclude();
        Config config = ConfigUtil.CONFIG;
        List<String> globalExcludeList = config.getExclude();

        Function<String, String> map = s -> {
            String subgroup = ReUtil.get(StringEnum.SUBGROUP_REG_STR, s, 1);
            if (StrUtil.isBlank(subgroup)) {
                return s;
            }
            if (subgroup.equals(ani.getSubgroup())) {
                return ReUtil.get(StringEnum.SUBGROUP_REG_STR, s, 2);
            }
            return "";
        };

        return Arrays.stream(torrentFile.getFilenames())
                .map(name -> {
                    name = CharsetUtil.convert(name, "ISO-8859-1", CharsetUtil.UTF_8);
                    name = ReUtil.replaceAll(name, "[\\\\/]$", "");
                    name = name.replace("\\", "/");
                    Item item = new Item();
                    return item.setTitle(name)
                            .setLength(lengths[index.getAndIncrement()]);
                })
                .filter(item -> {
                    String name = item.getTitle();

                    if (name.startsWith("_____padding_file_") && name.contains("BitComet")) {
                        return false;
                    }

                    // 排除
                    if (!exclude.isEmpty()) {
                        if (exclude.stream().map(map).filter(StrUtil::isNotBlank).anyMatch(s -> ReUtil.contains(s, name))) {
                            return false;
                        }
                    }

                    // 匹配
                    if (!match.isEmpty()) {
                        if (match.stream().map(map).filter(StrUtil::isNotBlank).anyMatch(s -> !ReUtil.contains(s, name))) {
                            return false;
                        }
                    }

                    // 全局排除
                    if (globalExclude) {
                        return globalExcludeList.stream().map(map).filter(StrUtil::isNotBlank).noneMatch(s -> ReUtil.contains(s, name));
                    }
                    return true;
                })
                .map(item -> {
                    long length = item.getLength();

                    Double l = length / 1024.0 / 1024;

                    String size = NumberUtil.decimalFormat("0.00", l) + "MB";

                    item
                            .setSize(size)
                            .setSubgroup(ani.getSubgroup());

                    RenameUtil.rename(ani, item);

                    String reName = item.getReName();

                    if (StrUtil.isBlank(reName)) {
                        return null;
                    }

                    String title = item.getTitle();

                    String extName = FileUtil.extName(title);

                    if (BaseDownload.subtitleFormat.contains(extName)) {
                        String lang = FileUtil.extName(FileUtil.mainName(title));
                        if (StrUtil.isNotBlank(lang)) {
                            reName += "." + lang;
                        }
                    }

                    reName = reName + "." + extName;

                    return item.setReName(reName)
                            .setLength(length);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static synchronized void download(String name, File torrentFile, String savePath, List<String> tags) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getDownloadToolHost();
        String download = config.getDownloadToolType();
        Assert.isTrue("qBittorrent".equals(download), "合集下载暂时只支持 qBittorrent");

        Assert.isTrue(TorrentUtil.login(), "下载器登录失败");

        Integer ratioLimit = config.getRatioLimit();
        Integer seedingTimeLimit = config.getSeedingTimeLimit();
        Integer inactiveSeedingTimeLimit = config.getInactiveSeedingTimeLimit();

        Long upLimit = config.getUpLimit() * 1024;
        Long dlLimit = config.getDlLimit() * 1024;

        Boolean qbUseDownloadPath = config.getQbUseDownloadPath();

        HttpReq.post(host + "/api/v2/torrents/add")
                .form("torrents", torrentFile)
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("category", "")
                .form("contentLayout", "Original")
                .form("dlLimit", dlLimit)
                .form("firstLastPiecePrio", false)
                .form("paused", true)
                .form("stopped", true)
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
                .then(HttpReq::assertStatus);
    }

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) {
        String type = request.getParam("type");

        CollectionInfo collectionInfo = getBody(CollectionInfo.class);
        String torrent = collectionInfo.getTorrent();

        // 预览
        if (type.equals("preview") || type.equals("subgroup")) {
            List<Item> preview = preview(collectionInfo);
            preview = CollUtil.sort(new ArrayList<>(preview), Comparator.comparingDouble(it -> {
                Double episode = it.getEpisode();
                return ObjectUtil.defaultIfNull(episode, 0.0);
            }));

            if (type.equals("subgroup")) {
                String subgroup = "未知字幕组";
                String reg = "^\\[(.+?)]";
                for (Item item : preview) {
                    String name = new File(item.getTitle()).getName();
                    if (!ReUtil.contains(reg, name)) {
                        continue;
                    }
                    subgroup = ReUtil.get(reg, name, 1);
                    break;
                }
                resultSuccess(subgroup);
                return;
            }
            resultSuccess(preview);
        }

        // 开始下载
        if (!type.equals("start")) {
            return;
        }

        Assert.isTrue(AfdianUtil.verifyExpirationTime(), "未解锁捐赠, 无法使用添加合集");

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

        String name = StrFormatter.format("[{}] {} 第{}季", subgroup, title, ani.getSeason());
        download(name, tempFile, downloadPath, List.of("ANI-RSS合集下载", subgroup));

        TorrentsInfo torrentsInfo = new TorrentsInfo()
                .setHash(torrentFile.getHexHash());

        Config config = ConfigUtil.CONFIG;

        List<qBittorrent.FileEntity> files = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ThreadUtil.sleep(500);
            try {
                files.addAll(qBittorrent.files(torrentsInfo, false, config));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (!files.isEmpty()) {
                // 添加下载完成
                break;
            }
        }

        Map<String, String> reNameMap = preview(collectionInfo)
                .stream()
                .map(item -> {
                    Optional<qBittorrent.FileEntity> fileEntity = files.stream()
                            .filter(f -> new File(f.getName()).getName().equals(new File(item.getTitle()).getName()))
                            .filter(f -> f.getSize().longValue() == item.getLength())
                            .findFirst();
                    if (fileEntity.isEmpty()) {
                        return null;
                    }
                    String oldPath = fileEntity.get().getName();
                    return item.setTitle(oldPath);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Item::getTitle,
                        Item::getReName
                ));

        String host = config.getDownloadToolHost();

        for (int i = 0; i < 30; i++) {
            for (qBittorrent.FileEntity file : files) {
                String oldPath = file.getName();
                String newPath = reNameMap.get(oldPath);

                if (!reNameMap.containsKey(oldPath)) {
                    if (!reNameMap.containsValue(oldPath) && file.getPriority() > 0) {
                        HttpReq.post(host + "/api/v2/torrents/filePrio")
                                .form("hash", torrentFile.getHexHash())
                                .form("id", file.getIndex())
                                .form("priority", 0)
                                .thenFunction(HttpResponse::isOk);
                    }
                    continue;
                }
                log.info("重命名 {} ==> {}", oldPath, newPath);
                HttpReq.post(host + "/api/v2/torrents/renameFile")
                        .form("hash", torrentFile.getHexHash())
                        .form("oldPath", oldPath)
                        .form("newPath", newPath)
                        .thenFunction(HttpResponse::isOk);
            }
            files.clear();
            files.addAll(qBittorrent.files(torrentsInfo, false, config));

            if (CollUtil.containsAll(files.stream()
                    .map(qBittorrent.FileEntity::getName)
                    .toList(), reNameMap.values())) {
                // 所有命名已完成
                break;
            }
            // 命名有遗漏 继续
            ThreadUtil.sleep(1000);
        }

        qBittorrent.start(torrentsInfo, config);
        resultSuccess(result ->
                result.setMessage("已经开始下载合集")
        );
    }


}
