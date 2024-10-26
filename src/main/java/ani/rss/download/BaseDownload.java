package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface BaseDownload {

    Cache<String, String> renameCache = CacheUtil.newFIFOCache(512);

    String tag = "ani-rss";

    List<String> videoFormat = List.of("mp4", "mkv", "avi", "wmv");

    List<String> subtitleFormat = List.of("ass", "ssa", "sub", "srt", "lyc");

    /**
     * 登录
     *
     * @return
     */
    Boolean login(Config config);

    /**
     * 获取任务列表
     *
     * @return
     */
    List<TorrentsInfo> getTorrentsInfos();

    /**
     * 下载
     *
     * @param item
     * @param savePath
     * @param torrentFile
     */
    Boolean download(Item item, String savePath, File torrentFile, Boolean ova);

    /**
     * 删除已完成任务
     *
     * @param torrentsInfo
     */
    void delete(TorrentsInfo torrentsInfo);

    /**
     * 重命名
     *
     * @param torrentsInfo
     */
    void rename(TorrentsInfo torrentsInfo);

    Boolean addTags(TorrentsInfo torrentsInfo, String tags);

    /**
     * 自动更新 Trackers
     */
    void updateTrackers(Set<String> trackers);

    default String getFileReName(String name, String reName) {
        String ext = FileUtil.extName(name);
        if (StrUtil.isBlank(ext)) {
            return name;
        }
        String newPath = reName;
        if (videoFormat.contains(ext.toLowerCase())) {
            newPath = newPath + "." + ext;
        } else if (subtitleFormat.contains(ext.toLowerCase())) {
            String s = FileUtil.extName(FileUtil.mainName(name));
            if (StrUtil.isNotBlank(s)) {
                newPath = newPath + "." + s;
            }
            newPath = newPath + "." + ext;
        } else {
            return name;
        }

        if (name.equals(newPath)) {
            return name;
        }
        return newPath;
    }
}
