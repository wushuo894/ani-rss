package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class AlistUtil {
    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    public static void upload(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        Boolean alist = config.getAlist();
        String alistHost = config.getAlistHost();
        String alistPath = config.getAlistPath();
        String alistToken = config.getAlistToken();

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

        String downloadDir = FileUtil.getAbsolutePath(torrentsInfo.getDownloadDir());

        String downloadPath = FileUtil.getAbsolutePath(config.getDownloadPath());
        String ovaDownloadPath = FileUtil.getAbsolutePath(config.getOvaDownloadPath());

        List<String> strings = torrentsInfo.getFiles().get();
        for (String string : strings) {
            String filePath = FileUtil.getAbsolutePath(alistPath);
            if (StrUtil.isNotBlank(downloadPath) && downloadDir.startsWith(downloadPath)) {
                filePath += downloadDir.substring(downloadPath.length());
            } else if (StrUtil.isNotBlank(ovaDownloadPath) && downloadDir.startsWith(ovaDownloadPath)) {
                filePath += downloadDir.substring(ovaDownloadPath.length());
            } else {
                filePath += downloadDir;
            }
            filePath += "/" + string;
            String finalFilePath = filePath;
            EXECUTOR.execute(() -> {
                File file = new File(downloadDir + "/" + string);
                if (!file.exists()) {
                    log.error("文件不存在 {}", file);
                    return;
                }
                log.info("上传 {} ==> {}", file, finalFilePath);
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
                            .put(url)
                            .timeout(1000 * 60 * 2)
                            .setConfig(httpConfig)
                            .header(Header.AUTHORIZATION, alistToken)
                            .header("As-Task", "true")
                            .header("File-Path", URLUtil.encode(finalFilePath))
                            .header(Header.CONTENT_LENGTH, String.valueOf(file.length()))
                            .form("file", file)
                            .then(res -> {
                                Assert.isTrue(res.isOk(), "上传失败 {} 状态码:{}", string, res.getStatus());
                                log.info("已向alist添加上传任务 {}", string);
                            });
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }
}
