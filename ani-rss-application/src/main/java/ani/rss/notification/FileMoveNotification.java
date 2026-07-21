package ani.rss.notification;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FileMoveNotification implements BaseNotification {
    private NotificationConfig notificationConfig;

    /**
     * 测试
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     */
    @Override
    public void test(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        List<NotificationStatusEnum> statusList = notificationConfig.getStatusList();

        Assert.isTrue(statusList.contains(NotificationStatusEnum.DOWNLOAD_END), "请设置为下载完成通知");
    }

    /**
     * 发送通知
     *
     * @param notificationConfig     通知配置
     * @param ani                    订阅
     * @param text                   通知内容
     * @param notificationStatusEnum 通知状态
     * @return 是否成功
     */
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        if (NotificationStatusEnum.DOWNLOAD_END != notificationStatusEnum) {
            log.info("文件移动 仅支持下载完成通知");
            return true;
        }

        this.notificationConfig = notificationConfig;

        // 首先就要深度克隆 防止影响原订阅设置
        ani = ObjectUtil.clone(ani);

        // 旧的位置
        DownloadService downloadService = SpringUtil.getBean(DownloadService.class);
        String src = downloadService.getDownloadPath(ani);

        // 新的位置; 设置自定义下载位置同时启用, 用以获取新的位置
        Boolean ova = ani.getOva();
        String fileMoveTarget = notificationConfig.getFileMoveTarget();
        String fileMoveOvaTarget = notificationConfig.getFileMoveOvaTarget();
        boolean fileMoveDeleteOldEpisode = notificationConfig.getFileMoveDeleteOldEpisode();

        String downloadPathTemplate = ova ? fileMoveOvaTarget : fileMoveTarget;
        String target = downloadService.getDownloadPath(ani, downloadPathTemplate);

        // 进行移动
        FileUtil.mkdir(target);

        if (ova) {
            startMoveOva(src, target);
            return true;
        }

        if (fileMoveDeleteOldEpisode) {
            deleteOldEpisode(src, target);
        }

        startMove(src, target);

        return true;
    }

    public void startMoveOva(String src, String target) {
        Boolean copyModel = notificationConfig.getFileMoveCopyModel();

        for (File file : FileUtils.listFiles(src)) {
            if (!file.isFile()) {
                continue;
            }

            log.info("OVA/剧场版 文件移动: {} => {}", file, target);
            FileUtil.copy(file, new File(target), true);

            if (copyModel) {
                continue;
            }
            // 复制后再删除 确保不会中途失败
            FileUtil.del(file);
        }
    }

    public void startMove(String src, String target) {
        Boolean copyModel = notificationConfig.getFileMoveCopyModel();

        // 目标位置文件列表 存储为MAP便于根据文件名寻找
        Map<String, File> targetFileMap = FileUtils.listFileList(target)
                .stream()
                .filter(File::isFile)
                .collect(Collectors.toMap(File::getName, i -> i));

        for (File file : FileUtils.listFileList(src)) {
            if (!file.isFile()) {
                // 过滤掉文件夹
                continue;
            }

            String name = file.getName();
            if (!FileUtils.isVideoFormat(name) && !FileUtils.isSubtitleFormat(name)) {
                // 非视频与字幕文件
                continue;
            }
            if (!ReUtil.contains(StringEnum.SEASON_REG, name)) {
                // 确保命名
                continue;
            }

            if (targetFileMap.containsKey(name)) {
                long targetFileLength = targetFileMap.get(name).length();
                long srcFileLength = file.length();
                if (targetFileLength == srcFileLength) {
                    // 文件名与大小一致 跳过移动
                    continue;
                }
            }

            log.info("文件移动: {} => {}", file, target);
            FileUtil.copy(file, new File(target), true);

            if (copyModel) {
                continue;
            }

            // 复制后再删除 确保不会中途失败
            FileUtil.del(file);
        }
    }

    /**
     * 删除目标位置的同集视频, 以实现洗版效果
     *
     * @param src    原位置
     * @param target 新位置
     */
    public void deleteOldEpisode(String src, String target) {
        // 原位置文件列表
        List<File> srcFileList = FileUtils.listFileList(src)
                .stream()
                .filter(FileUtil::isFile)
                .toList();

        // 存储为MAP便于根据文件名寻找
        Map<String, File> srcFileMap = srcFileList.stream()
                .collect(Collectors.toMap(File::getName, i -> i));

        // 集数列表: S01E01, S01E02 ~
        Set<String> episodeSet = srcFileList
                .stream()
                .map(File::getName)
                .filter(name -> FileUtils.isVideoFormat(name) || FileUtils.isSubtitleFormat(name))
                .filter(name -> ReUtil.contains(StringEnum.SEASON_REG, name))
                .map(name -> ReUtil.get(StringEnum.SEASON_REG, name, 0))
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<File> files = FileUtils.listFileList(target);
        for (File file : files) {
            String name = file.getName();
            if (!FileUtils.isVideoFormat(name) && !FileUtils.isSubtitleFormat(name)) {
                // 非视频与字幕文件
                continue;
            }
            if (!ReUtil.contains(StringEnum.SEASON_REG, name)) {
                // 确保命名
                continue;
            }

            if (srcFileMap.containsKey(name)) {
                long targetFileLength = file.length();
                long srcFileLength = srcFileMap.get(name).length();
                if (targetFileLength == srcFileLength) {
                    // 文件名与大小一致 跳过删除
                    continue;
                }
            }

            String episode = ReUtil.get(StringEnum.SEASON_REG, name, 0);
            episode = episode.toUpperCase();
            if (!episodeSet.contains(episode)) {
                // 新位置没有旧的同集文件, 不需要删除
                continue;
            }

            log.info("因洗版需要删除: {}", file);

            // 删除 洗版
            FileUtil.del(file);

        }

    }

}
