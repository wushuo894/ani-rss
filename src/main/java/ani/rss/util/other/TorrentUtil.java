package ani.rss.util.other;

import ani.rss.action.ClearCacheAction;
import ani.rss.download.BaseDownload;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.enums.TorrentsTags;
import ani.rss.util.basic.ExceptionUtil;
import ani.rss.util.basic.FilePathUtil;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.bittorrent.TorrentFile;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * 管理下载器的调用与种子存取
 */
@Slf4j
public class TorrentUtil {
    public static BaseDownload DOWNLOAD;

    /**
     * 获取任务列表
     *
     * @return
     */
    public static synchronized List<TorrentsInfo> getTorrentsInfos() {
        ThreadUtil.sleep(1000);
        return DOWNLOAD.getTorrentsInfos();
    }

    /**
     * 获取种子存放文件夹
     *
     * @param ani
     * @return
     */
    public static File getTorrentDir(Ani ani) {
        String title = ani.getTitle();
        Boolean ova = ani.getOva();
        Integer season = ani.getSeason();

        File configDir = ConfigUtil.getConfigDir();

        String pinyin = PinyinUtil.getPinyin(title);
        String s = pinyin.toUpperCase().substring(0, 1);
        if (ReUtil.isMatch("^\\d$", s)) {
            s = "0";
        } else if (!ReUtil.isMatch("^[a-zA-Z]$", s)) {
            s = "#";
        }

        File torrents = new File(StrFormatter.format("{}/torrents/{}/Season {}", configDir, title, season));
        if (!torrents.exists()) {
            torrents = new File(StrFormatter.format("{}/torrents/{}/{}/Season {}", configDir, s, title, season));
        }
        if (ova) {
            torrents = new File(StrFormatter.format("{}/torrents/{}", configDir, title));
            if (!torrents.exists()) {
                torrents = new File(StrFormatter.format("{}/torrents/{}/{}", configDir, s, title));
            }
        }
        FileUtil.mkdir(torrents);
        return torrents;
    }

    /**
     * 获取种子
     *
     * @param ani
     * @param item
     * @return
     */
    public static File getTorrent(Ani ani, Item item) {
        String infoHash = item.getInfoHash();
        File torrents = getTorrentDir(ani);
        String torrent = item.getTorrent();
        if (ReUtil.contains(StringEnum.MAGNET_REG, torrent)) {
            return new File(torrents + "/" + infoHash + ".txt");
        }
        return new File(torrents + "/" + infoHash + ".torrent");
    }

    /**
     * 下载种子文件
     *
     * @param item
     */
    public static File saveTorrent(Ani ani, Item item) {
        String torrent = item.getTorrent();
        String reName = item.getReName();

        log.info("下载种子 {}", reName);
        File saveTorrentFile = getTorrent(ani, item);
        if (saveTorrentFile.exists()) {
            return saveTorrentFile;
        }

        try {
            if (ReUtil.contains(StringEnum.MAGNET_REG, torrent)) {
                FileUtil.writeUtf8String(torrent, saveTorrentFile);
                log.info("种子下载完成 {}", reName);
                return saveTorrentFile;
            }

            return HttpReq.get(torrent)
                    .thenFunction(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            // 如果为 404 则写入空文件 已在 getMagnet 处理过
                            FileUtil.writeUtf8String("", saveTorrentFile);
                            log.info("种子下载完成 {}", reName);
                            return saveTorrentFile;
                        }
                        HttpReq.assertStatus(res);
                        FileUtil.writeFromStream(res.bodyStream(), saveTorrentFile, true);
                        log.info("种子下载完成 {}", reName);
                        return saveTorrentFile;
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error("下载种子时出现问题 {}", message);
            log.error(message, e);
            // 种子未下载异常，删除
            FileUtil.del(saveTorrentFile);
        }
        return saveTorrentFile;
    }

    /**
     * 登录 qBittorrent
     *
     * @return
     */
    public static synchronized Boolean login() {
        ThreadUtil.sleep(1000);
        Config config = ConfigUtil.CONFIG;
        String downloadPath = config.getDownloadPathTemplate();
        if (StrUtil.isBlank(downloadPath)) {
            log.warn("下载位置未设置");
            return false;
        }
        try {
            return DOWNLOAD.login(ConfigUtil.CONFIG);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断种子是否可以删除
     *
     * @param torrentsInfo
     * @return
     */
    public static Boolean isDelete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean awaitStalledUP = config.getAwaitStalledUP();

        TorrentsInfo.State state = torrentsInfo.getState();

        if (Objects.isNull(state)) {
            return false;
        }

        // 是否等待做种完毕
        if (awaitStalledUP) {
            return List.of(
                    TorrentsInfo.State.pausedUP.name(),
                    TorrentsInfo.State.stoppedUP.name()
            ).contains(state.name());
        }

        return List.of(
                TorrentsInfo.State.queuedUP.name(),
                TorrentsInfo.State.uploading.name(),
                TorrentsInfo.State.stalledUP.name(),
                TorrentsInfo.State.pausedUP.name(),
                TorrentsInfo.State.stoppedUP.name()
        ).contains(state.name());
    }


    /**
     * 删除已完成任务
     *
     * @param torrentsInfo 任务
     * @param forcedDelete 强制删除
     * @param deleteFiles  删除本地文件
     */
    public static synchronized Boolean delete(TorrentsInfo torrentsInfo, Boolean forcedDelete, Boolean deleteFiles) {
        Config config = ConfigUtil.CONFIG;
        Boolean delete = config.getDelete();

        String name = torrentsInfo.getName();

        if (forcedDelete) {
            log.info("删除任务 {}", name);
        } else {
            if (!isDelete(torrentsInfo)) {
                return false;
            }
            if (!delete) {
                return false;
            }
            log.info("删除已完成任务 {}", name);
        }
        ThreadUtil.sleep(500);
        List<String> files = torrentsInfo.getFiles().get();

        Boolean b = DOWNLOAD.delete(torrentsInfo, deleteFiles);
        if (!b) {
            log.error("删除任务失败 {}", name);
            return false;
        }
        log.info("删除任务成功 {}", name);
        if (!deleteFiles) {
            return true;
        }
        // 清理空文件夹
        ClearCacheAction.clearParentFile(new File(torrentsInfo.getDownloadDir() + "/" + name));
        return true;
    }


    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    public static synchronized Boolean delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean deleteFiles = config.getDeleteFiles();
        Boolean alist = config.getAlist();
        if (!deleteFiles || !alist) {
            return delete(torrentsInfo, false, false);
        }
        // 开启 alist上传 后删除源文件的行为需要等待 alist 上传完成
        if (torrentsInfo.getTags().contains(TorrentsTags.UPLOAD_COMPLETED.getValue())) {
            return delete(torrentsInfo, false, true);
        }
        return false;
    }

    /**
     * 重命名
     *
     * @param torrentsInfo
     */
    public static synchronized void rename(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean rename = config.getRename();
        if (!rename) {
            return;
        }

        List<String> tags = torrentsInfo.getTags();
        if (tags.contains(TorrentsTags.RENAME.getValue())) {
            return;
        }

        ThreadUtil.sleep(1000);
        DOWNLOAD.rename(torrentsInfo);
        addTags(torrentsInfo, TorrentsTags.RENAME.getValue());
    }

    /**
     * 添加标签
     *
     * @param torrentsInfo
     * @param tags
     * @return
     */
    public static Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        if (StrUtil.isBlank(tags)) {
            return false;
        }
        String name = torrentsInfo.getName();
        log.debug("添加标签 {} {}", name, tags);
        boolean b = false;
        try {
            b = DOWNLOAD.addTags(torrentsInfo, tags);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return b;
    }


    /**
     * 修改保存位置
     *
     * @param torrentsInfo
     * @param path
     */
    public static void setSavePath(TorrentsInfo torrentsInfo, String path) {
        if (StrUtil.isBlank(path)) {
            return;
        }
        try {
            log.info("修改保存位置 {} ==> {}", torrentsInfo.getName(), path);
            DOWNLOAD.setSavePath(torrentsInfo, path);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static synchronized void load() {
        Config config = ConfigUtil.CONFIG;
        String download = config.getDownloadToolType();

        if (download.equals("Alist")) {
            download = "OpenList";
            config.setDownloadToolType(download);
            ConfigUtil.sync();
        }

        DOWNLOAD = ReflectUtil.newInstance("ani.rss.download." + download);
        log.info("下载工具 {}", download);
    }

    /**
     * 通过种子获取到磁力链接
     *
     * @param file
     * @return
     */
    public static synchronized String getMagnet(File file) {
        String hexHash = FileUtil.mainName(file);
        if (file.length() < 1) {
            return StrFormatter.format("magnet:?xt=urn:btih:{}", hexHash);
        }
        if (FileUtil.extName(file).equals("txt")) {
            return FileUtil.readUtf8String(file);
        }
        try {
            TorrentFile torrentFile = new TorrentFile(file);
            hexHash = torrentFile.getHexHash();
        } catch (Exception e) {
            log.error("转换种子为磁力链接时出现错误 {}", FilePathUtil.getAbsolutePath(file));
            log.error(e.getMessage(), e);
        }
        return StrFormatter.format("magnet:?xt=urn:btih:{}", hexHash);
    }

}
