package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.TorrentsTags;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConfig;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Alist
 */
@Slf4j
public class AlistUtil {
    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    /**
     * 将下载完成的任务上传至Alist
     *
     * @param torrentsInfo 任务
     */
    public static void upload(TorrentsInfo torrentsInfo, Ani ani) {
        Boolean upload = Opt.ofNullable(ani)
                .map(Ani::getUpload)
                .orElse(true);
        // 禁止自动上传
        if (!upload) {
            return;
        }

        Config config = ConfigUtil.CONFIG;
        Boolean alist = config.getAlist();
        if (!alist) {
            return;
        }
        String alistHost = config.getAlistHost();
        String alistToken = config.getAlistToken();
        Integer alistRetry = config.getAlistRetry();

        verify();

        List<String> tags = torrentsInfo.getTags();
        if (tags.contains(TorrentsTags.A_LIST.getValue())) {
            return;
        }
        if (tags.contains(TorrentsTags.UPLOAD_COMPLETED.getValue())) {
            return;
        }

        TorrentUtil.addTags(torrentsInfo, TorrentsTags.A_LIST.getValue());

        String downloadDir = FilePathUtil.getAbsolutePath(torrentsInfo.getDownloadDir());

        List<String> files = torrentsInfo.getFiles().get();
        String filePath = getPath(ani);
        for (String fileName : files) {
            String finalFilePath = filePath + "/" + fileName;
            File file = new File(downloadDir + "/" + fileName);
            if (!file.exists()) {
                log.error("文件不存在 {}", file);
                return;
            }

            EXECUTOR.execute(() -> {
                log.info("上传 {} ==> {}", file, finalFilePath);

                Boolean alistTask = config.getAlistTask();

                for (int i = 0; i < alistRetry; i++) {
                    try {
                        String url = alistHost;
                        // 使用流式上传
                        url += "/api/fs/form";

                        // 50M 上传
                        HttpConfig httpConfig = new HttpConfig()
                                .setBlockSize(1024 * 1024 * 50);

                        HttpReq
                                .put(url, false)
                                .timeout(1000 * 60 * 2)
                                .setConfig(httpConfig)
                                .header(Header.AUTHORIZATION, alistToken)
                                .header("As-Task", Boolean.toString(alistTask))
                                .header("File-Path", URLUtil.encode(finalFilePath))
                                .header(Header.CONTENT_LENGTH, String.valueOf(file.length()))
                                .form("file", file)
                                .then(res -> {
                                    Assert.isTrue(res.isOk(), "上传失败 {} 状态码:{}", fileName, res.getStatus());
                                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                                    int code = jsonObject.get("code").getAsInt();
                                    log.info(jsonObject.toString());
                                    Assert.isTrue(code == 200, "上传失败 {} 状态码:{}", fileName, code);

                                    String text = StrFormatter.format("alist上传完成 {}", fileName);
                                    if (alistTask) {
                                        text = StrFormatter.format("已向alist添加上传任务 {}", fileName);
                                    }
                                    log.info(text);
                                    NotificationUtil.send(config, ani, text, NotificationStatusEnum.ALIST_UPLOAD);
                                });
                        TorrentUtil.addTags(torrentsInfo, TorrentsTags.UPLOAD_COMPLETED.getValue());
                        return;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                if (AfdianUtil.verifyExpirationTime()) {
                    NotificationUtil.send(config, ani, "alist上传失败 " + fileName, NotificationStatusEnum.ERROR);
                }
            });
        }
    }

    /**
     * 刷新 Alist 路径
     */
    public static void refresh(Ani ani) {
        Config config = ConfigUtil.CONFIG;
        Boolean refresh = config.getAlistRefresh();
        if (!refresh) {
            return;
        }
        String alistHost = config.getAlistHost();
        String alistToken = config.getAlistToken();

        verify();

        String finalPath = getPath(ani);
        EXECUTOR.execute(() -> {
            Long getAlistRefreshDelay = config.getAlistRefreshDelayed();
            if (getAlistRefreshDelay > 0) {
                ThreadUtil.sleep(getAlistRefreshDelay, TimeUnit.SECONDS);
            }
            log.info("刷新 Alist 路径: {}", finalPath);

            try {
                HttpReq
                        .post(alistHost + "/api/fs/mkdir", false)
                        .header(Header.AUTHORIZATION, alistToken)
                        .body(GsonStatic.toJson(Map.of("path", finalPath)))
                        .then(HttpReq::assertStatus);

                String url = alistHost;
                url += "/api/fs/list";

                Map<String, Object> resolved = Map.of(
                        "path", finalPath,
                        "refresh", true
                );
                HttpReq.post(url)
                        .timeout(1000 * 20)
                        .header(Header.AUTHORIZATION, alistToken)
                        .body(GsonStatic.toJson(resolved))
                        .then(res -> {
                            Assert.isTrue(res.isOk(), "刷新失败 路径: {} 状态码: {}", finalPath, res.getStatus());
                            JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                            int code = jsonObject.get("code").getAsInt();
                            Assert.isTrue(code == 200, "刷新失败 路径: {} 状态码: {}", finalPath, code);
                            log.info("已成功刷新 Alist 路径: {}", finalPath);
                        });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            ThreadUtil.sleep(3000);
        });
    }

    /**
     * 校验配置
     */
    public static void verify() {
        Config config = ConfigUtil.CONFIG;
        String alistHost = config.getAlistHost();
        String alistPath = FilePathUtil.getAbsolutePath(config.getAlistPath());
        String alistToken = config.getAlistToken();

        Assert.notBlank(alistHost, "alistHost 未配置");
        Assert.notBlank(alistPath, "alistPath 未配置");
        Assert.notBlank(alistToken, "alistToken 未配置");
    }

    /**
     * 获取上传位置
     *
     * @param ani
     * @return
     */
    private static String getPath(Ani ani) {
        ani = ObjectUtil.clone(ani)
                // 因为临时修改下载位置模版以获取对应下载位置, 要关闭自定义下载位置
                .setCustomDownloadPath(false);
        Config config = ObjectUtil.clone(ConfigUtil.CONFIG);

        config.setDownloadPathTemplate(config.getAlistPath())
                .setOvaDownloadPathTemplate(config.getAlistOvaPath());

        String path = FilePathUtil.getAbsolutePath(TorrentUtil.getDownloadPath(ani, config));

        path = ReUtil.replaceAll(path, "^[A-z]:", "");

        return path;
    }
}
