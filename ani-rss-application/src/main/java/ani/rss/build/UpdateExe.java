package ani.rss.build;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class UpdateExe implements Runnable {
    @Override
    public void run() {
        String basedir = System.getProperty("basedir");
        File updateExeFile = new File(basedir, "/src/main/resources/ani-rss-update.exe");

        if (updateExeFile.exists()) {
            log.info("文件已存在 {}", updateExeFile);
            return;
        }

        String downloadUrl = "https://github.com/wushuo894/ani-rss-update/releases/download/latest/ani-rss-update.exe";

        log.info("下载 {}", downloadUrl);

        try {
            HttpUtil.downloadFile(downloadUrl, updateExeFile, new StreamProgress() {
                @Override
                public void start() {
                }

                @Override
                public void progress(long total, long progressSize) {
                    int progress = Double.valueOf(progressSize * 1.0 / total * 100).intValue();
                    System.out.print("\r下载进度: " + progress + "%");
                }

                @Override
                public void finish() {
                    System.out.println();
                }
            });
            log.info("下载成功 {}", updateExeFile);
        } catch (Exception e) {
            log.error("下载失败", e);
        }
    }
}
