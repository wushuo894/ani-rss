package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.TorrentsTags;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConfig;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

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
        String alistHost = config.getAlistHost();
        String alistPath = FileUtil.getAbsolutePath(config.getAlistPath());
        String alistOvaPath = FileUtil.getAbsolutePath(config.getAlistOvaPath());
        String alistToken = config.getAlistToken();
        Integer alistRetry = config.getAlistRetry();

        if (!alist) {
            return;
        }

        if (StrUtil.isBlank(alistHost)) {
            log.error("alistHost 未配置");
            return;
        }
        if (StrUtil.isBlank(alistToken)) {
            log.error("alistToken 未配置");
            return;
        }
        if (StrUtil.isBlank(alistPath)) {
            log.error("alistPath 未配置");
            return;
        }

        List<String> tags = torrentsInfo.getTags();
        if (tags.contains(TorrentsTags.A_LIST.getValue())) {
            return;
        }

        TorrentUtil.addTags(torrentsInfo, TorrentsTags.A_LIST.getValue());

        String downloadDir = FileUtil.getAbsolutePath(torrentsInfo.getDownloadDir());

        String downloadPath = FileUtil.getAbsolutePath(config.getDownloadPath());
        String ovaDownloadPath = FileUtil.getAbsolutePath(config.getOvaDownloadPath());

        List<String> files = torrentsInfo.getFiles().get();
        for (String fileName : files) {
            String filePath = alistPath;

            Boolean ova = Opt.ofNullable(ani)
                    .map(Ani::getOva)
                    .orElse(false);
            if (ova) {
                filePath = StrUtil.blankToDefault(alistOvaPath, filePath);
            }

            if (StrUtil.isNotBlank(downloadPath) && downloadDir.startsWith(downloadPath)) {
                filePath += downloadDir.substring(downloadPath.length());
            } else if (StrUtil.isNotBlank(ovaDownloadPath) && downloadDir.startsWith(ovaDownloadPath)) {
                filePath += downloadDir.substring(ovaDownloadPath.length());
            } else {
                filePath += downloadDir;
            }
            filePath += "/" + fileName;
            String finalFilePath = filePath;
            File file = new File(downloadDir + "/" + fileName);
            if (!file.exists()) {
                log.error("文件不存在 {}", file);
                return;
            }

            EXECUTOR.execute(() -> {
                log.info("上传 {} ==> {}", file, finalFilePath);
                for (int i = 0; i < alistRetry; i++) {
                    try {
                        String url = alistHost;
                        if (url.endsWith("/")) {
                            url = url.substring(0, url.length() - 1);
                        }
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
                                .header("As-Task", "true")
                                .header("File-Path", URLUtil.encode(finalFilePath))
                                .header(Header.CONTENT_LENGTH, String.valueOf(file.length()))
                                .form("file", file)
                                .then(res -> {
                                    Assert.isTrue(res.isOk(), "上传失败 {} 状态码:{}", fileName, res.getStatus());
                                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                                    int code = jsonObject.get("code").getAsInt();
                                    log.info(jsonObject.toString());
                                    Assert.isTrue(code == 200, "上传失败 {} 状态码:{}", fileName, code);
                                    log.info("已向alist添加上传任务 {}", fileName);
                                });
                        return;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }
    }
}
