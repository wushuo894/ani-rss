package ani.rss.notification;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.OpenListFileInfo;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConfig;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class OpenListUploadNotification implements BaseNotification {
    /**
     * 上传配置
     */
    private final HttpConfig httpConfig = new HttpConfig()
            .setBlockSize(1024 * 1024 * 50);
    private NotificationConfig notificationConfig;

    @Override
    public Boolean send(NotificationConfig notificationConfig, Ani ani, String text, NotificationStatusEnum notificationStatusEnum) {
        Assert.isTrue(NotificationStatusEnum.DOWNLOAD_END == notificationStatusEnum, "OpenListUpload 仅支持下载完成通知");

        ani = ObjectUtil.clone(ani);

        this.notificationConfig = notificationConfig;

        String openListUploadPath = notificationConfig.getOpenListUploadPath();
        String openListUploadOvaPath = notificationConfig.getOpenListUploadOvaPath();
        Boolean deleteOldEpisode = notificationConfig.getOpenListUploadDeleteOldEpisode();

        // 本地位置
        String localPath = DownloadService.getDownloadPath(ani);

        // 新的位置; 设置自定义下载位置同时启用, 用以获取新的位置
        Boolean ova = ani.getOva();
        if (ova) {
            ani.setDownloadPath(openListUploadOvaPath);
        } else {
            ani.setDownloadPath(openListUploadPath);
        }

        Boolean customUploadEnable = ani.getCustomUploadEnable();
        if (customUploadEnable) {
            // 自定义上传位置
            ani.setDownloadPath(ani.getCustomUploadPathTarget());
        }

        ani.setCustomDownloadPath(true);

        String target = DownloadService.getDownloadPath(ani);
        target = ReUtil.replaceAll(target, "^[A-z]:", "");

        if (ova) {
            uploadOva(localPath, target);
            return true;
        }

        if (deleteOldEpisode) {
            deleteOldEpisode(localPath, target);
        }

        upload(localPath, target);

        return true;
    }

    public void uploadOva(String localFilePath, String cloudFilePath) {
        List<File> files = FileUtils.listFileList(localFilePath);
        for (File file : files) {
            if (file.isDirectory()) {
                // 文件夹 跳过
                continue;
            }
            String name = file.getName();

            if (!FileUtils.isSubtitleFormat(name) && !FileUtils.isVideoFormat(name)) {
                // 非视频与字幕 跳过
                continue;
            }

            // 开始上传
            uploadFile(file.getAbsolutePath(), cloudFilePath);
        }
    }

    public void deleteOldEpisode(String localFilePath, String cloudFilePath) {
        Set<String> episodeSet = FileUtils.listFileList(localFilePath)
                .stream()
                .filter(FileUtil::isFile)
                .map(File::getName)
                .filter(name -> FileUtils.isVideoFormat(name) || FileUtils.isSubtitleFormat(name))
                .filter(name -> ReUtil.contains(StringEnum.SEASON_REG, name))
                .map(name -> ReUtil.get(StringEnum.SEASON_REG, name, 0))
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<OpenListFileInfo> fileInfos = fileList(cloudFilePath);
        for (OpenListFileInfo fileInfo : fileInfos) {
            String name = fileInfo.getName();
            if (!FileUtils.isVideoFormat(name) && !FileUtils.isSubtitleFormat(name)) {
                // 非视频与字幕文件
                continue;
            }
            if (!ReUtil.contains(StringEnum.SEASON_REG, name)) {
                // 确保命名
                continue;
            }

            String episode = ReUtil.get(StringEnum.SEASON_REG, name, 0);
            episode = episode.toUpperCase();
            if (!episodeSet.contains(episode)) {
                // 新位置没有旧的同集文件, 不需要删除
                continue;
            }

            log.info("因洗版需要删除: {}", name);

            // 删除 洗版
            postApi("fs/remove")
                    .body(GsonStatic.toJson(Map.of(
                            "dir", cloudFilePath,
                            "names", List.of(name)
                    ))).then(HttpResponse::isOk);
        }
    }

    private void upload(String localFilePath, String cloudFilePath) {
        Boolean openListUploadDeleteLocalFile = notificationConfig.getOpenListUploadDeleteLocalFile();

        for (File file : FileUtils.listFileList(localFilePath)) {
            if (file.isDirectory()) {
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
            log.info("文件上传: {} => {}", file, cloudFilePath);

            uploadFile(file.getAbsolutePath(), cloudFilePath);

            if (openListUploadDeleteLocalFile) {
                log.info("删除本地文件 {}", file);
                FileUtil.del(file);
            }
        }
    }


    /**
     * 上传文件
     *
     * @param localFilePath 本地文件位置
     * @param cloudFilePath 云端文件位置
     */
    private void uploadFile(String localFilePath, String cloudFilePath) {
        Assert.isTrue(FileUtil.exist(localFilePath), "文件不存在 {}", localFilePath);

        if (FileUtil.isDirectory(localFilePath)) {
            List<File> files = FileUtils.listFileList(localFilePath);
            for (File file : files) {
                uploadFile(file.getAbsolutePath(), cloudFilePath);
            }
            return;
        }

        String openListUploadHost = notificationConfig.getOpenListUploadHost();
        String openListUploadApiKey = notificationConfig.getOpenListUploadApiKey();
        Boolean openListUploadTask = notificationConfig.getOpenListUploadTask();

        String url = StrUtil.format("{}/api/fs/put", openListUploadHost);


        String filename = FileUtil.getName(localFilePath);

        HttpReq
                .put(url)
                .timeout(1000 * 60 * 2)
                .setConfig(httpConfig)
                .header(Header.AUTHORIZATION, openListUploadApiKey)
                .header("As-Task", Boolean.toString(openListUploadTask))
                .header("File-Path", URLUtil.encode(cloudFilePath + "/" + filename))
                .contentType("application/octet-stream")
                .body(ResourceUtil.getResourceObj(localFilePath))
                .then(res -> {
                    Assert.isTrue(res.isOk(), "上传失败 {} 状态码:{}", localFilePath, res.getStatus());
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    int code = jsonObject.get("code").getAsInt();
                    log.info(jsonObject.toString());
                    Assert.isTrue(code == 200, "上传失败 {} 状态码:{}", localFilePath, code);

                    String text = StrFormatter.format("OpenList 上传完成 {}", filename);
                    if (openListUploadTask) {
                        text = StrFormatter.format("已向 OpenList 添加上传任务 {}", filename);
                    }
                    log.info(text);
                });
    }

    /**
     * 文件列表
     *
     * @param path 目录
     * @return 文件列表
     */
    public List<OpenListFileInfo> fileList(String path) {
        try {
            return postApi("fs/list")
                    .body(GsonStatic.toJson(Map.of(
                            "path", path,
                            "page", 1,
                            "per_page", 256,
                            "refresh", true
                    )))
                    .thenFunction(res -> {
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        int code = jsonObject.get("code").getAsInt();
                        if (code != 200) {
                            return List.of();
                        }
                        JsonElement data = jsonObject.get("data");
                        if (Objects.isNull(data) || data.isJsonNull()) {
                            return List.of();
                        }
                        JsonElement content = data.getAsJsonObject()
                                .get("content");
                        if (Objects.isNull(content) || content.isJsonNull()) {
                            return List.of();
                        }
                        List<OpenListFileInfo> infos = GsonStatic.fromJsonList(content.getAsJsonArray(), OpenListFileInfo.class);
                        for (OpenListFileInfo info : infos) {
                            info.setPath(path);
                        }
                        return ListUtil.sort(new ArrayList<>(infos), Comparator.comparing(fileInfo -> {
                            Long size = fileInfo.getSize();
                            return Long.MAX_VALUE - ObjectUtil.defaultIfNull(size, 0L);
                        }));
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return List.of();
    }

    /**
     * post api
     *
     * @param action 操作
     * @return HttpReq
     */
    public synchronized HttpRequest postApi(String action) {
        ThreadUtil.sleep(2000);
        String openListUploadHost = notificationConfig.getOpenListUploadHost();
        String openListUploadApiKey = notificationConfig.getOpenListUploadApiKey();
        return HttpReq.post(openListUploadHost + "/api/" + action)
                .header(Header.AUTHORIZATION, openListUploadApiKey);
    }
}
