package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

        if (alistPath.endsWith("/")) {
            alistPath = alistPath.substring(0, alistPath.length() - 1);
        }

        String downloadDir = torrentsInfo.getDownloadDir();

        String downloadPath = config.getDownloadPath();

        List<String> strings = torrentsInfo.getFiles().get();
        for (String string : strings) {
            String filePath = alistPath;
            if (downloadDir.startsWith(downloadPath)) {
                filePath += downloadDir.substring(downloadPath.length());
            } else {
                filePath += downloadDir;
            }
            filePath += "/" + string;
            String finalFilePath = filePath;
            EXECUTOR.execute(() -> {
                log.info("上传 {} ==> {}", string, finalFilePath);
                try {
                    String url = alistHost;
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    url += "/api/fs/form";
                    HttpReq
                            .put(url)
                            .timeout(1000 * 60 * 2)
                            .header(Header.AUTHORIZATION, alistToken)
                            .header("As-Task", "true")
                            .header("File-Path", URLUtil.encode(finalFilePath))
                            .form("file", new File(downloadDir + "/" + string))
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
