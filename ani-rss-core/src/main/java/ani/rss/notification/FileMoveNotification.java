package ani.rss.notification;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.service.DownloadService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class FileMoveNotification implements BaseNotification {
    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Assert.isTrue(NotificationStatusEnum.DOWNLOAD_END == notificationStatusEnum, "文件移动仅支持下载完成通知");

        // 首先就要深度克隆 防止影响原订阅设置
        ani = ObjectUtil.clone(ani);

        // 旧的位置
        String src = DownloadService.getDownloadPath(ani);

        // 新的位置; 设置自定义下载位置同时启用, 用以获取新的位置
        Boolean ova = ani.getOva();
        String fileMoveTarget = notificationConfig.getFileMoveTarget();
        String fileMoveOvaTarget = notificationConfig.getFileMoveOvaTarget();
        if (ova) {
            ani.setDownloadPath(fileMoveOvaTarget);
        } else {
            ani.setDownloadPath(fileMoveTarget);
        }
        ani.setCustomDownloadPath(true);

        String target = DownloadService.getDownloadPath(ani);

        // 进行移动
        FileUtil.mkdir(target);

        for (File file : FileUtils.listFiles(src)) {
            if (file.isDirectory()) {
                continue;
            }

            log.info("文件移动: {} => {}", file, target);
            FileUtil.copy(file, new File(target), true);

            // 复制后再删除 确保不会中途失败
            FileUtil.del(file);
        }

        return true;
    }

}
