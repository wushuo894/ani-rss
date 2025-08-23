package ani.rss.download;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.TorrentsTags;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface BaseDownload {

    /**
     * 视频格式
     */
    List<String> videoFormat = List.of("mp4", "mkv", "avi", "wmv");

    /**
     * 字幕格式
     */
    List<String> subtitleFormat = List.of("ass", "ssa", "sub", "srt", "lyc");

    /**
     * 登录
     *
     * @param config 设置
     * @return 登录状态
     */
    default Boolean login(Config config) {
        return login(false, config);
    }

    /**
     * 登录
     *
     * @param test   测试登录
     * @param config 设置
     * @return 登录状态
     */
    Boolean login(Boolean test, Config config);

    /**
     * 获取任务列表
     *
     * @return 任务列表
     */
    List<TorrentsInfo> getTorrentsInfos();

    /**
     * 下载
     *
     * @param ani         订阅
     * @param item        下载项
     * @param savePath    保存位置
     * @param torrentFile 种子文件
     * @param ova         是否剧场版/OVA
     * @return 下载状态
     */
    Boolean download(Ani ani, Item item, String savePath, File torrentFile, Boolean ova);

    /**
     * 删除已完成任务
     *
     * @param torrentsInfo 任务
     * @param deleteFiles  删除本地文件
     * @return 删除状态
     */
    Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles);

    /**
     * 重命名
     *
     * @param torrentsInfo 任务
     */
    void rename(TorrentsInfo torrentsInfo);

    /**
     * 为任务添加标签
     *
     * @param torrentsInfo 任务
     * @param tags         标签
     * @return 状态
     */
    Boolean addTags(TorrentsInfo torrentsInfo, String tags);

    /**
     * 自动更新 Trackers
     *
     * @param trackers trackers 列表
     */
    void updateTrackers(Set<String> trackers);

    /**
     * 修改保存位置
     *
     * @param torrentsInfo 任务
     * @param path         位置
     */
    void setSavePath(TorrentsInfo torrentsInfo, String path);

    /**
     * 获取重命名结果
     *
     * @param name   文件名
     * @param reName 重命名
     * @return 最终命名
     */
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

    /**
     * 获取新任务的tag
     *
     * @param ani
     * @param item
     * @return
     */
    default List<String> newTags(Ani ani, Item item) {
        Boolean master = item.getMaster();
        String subgroup = item.getSubgroup();
        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");

        Config config = ConfigUtil.CONFIG;

        List<String> tags = new ArrayList<>();

        tags.add(TorrentsTags.ANI_RSS.getValue());
        tags.add(subgroup);
        if (!master) {
            tags.add(TorrentsTags.BACK_RSS.getValue());
        }

        Boolean customTagsEnable = ani.getCustomTagsEnable();

        if (customTagsEnable) {
            // 获取订阅自定义标签
            List<String> aniCustomTags = ani.getCustomTags();
            if (CollectionUtil.isNotEmpty(aniCustomTags)) {
                tags.addAll(aniCustomTags);
            }
            return tags;
        }

        // 获取全局自定义标签
        List<String> globalCustomTags = config.getCustomTags();
        if (CollectionUtil.isNotEmpty(globalCustomTags)) {
            tags.addAll(globalCustomTags);
        }

        return tags;
    }
}
