package ani.rss.util;

import ani.rss.entity.About;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;

@Slf4j
public class UpdateUtil {
    public static About about() {
        String version = MavenUtil.getVersion();

        About about = new About()
                .setVersion(version)
                .setUpdate(false)
                .setLatest("")
                .setMarkdownBody("");
        try {
            HttpReq.get("https://github.com/wushuo894/ani-rss/releases/latest", true)
                    .timeout(3000)
                    .then(response -> {
                        String body = response.body();
                        Document document = Jsoup.parse(body);
                        Element box = document.selectFirst(".Box");
                        Element element = box.selectFirst("h1");
                        String latest = element.text().replace("v", "").trim();
                        about.setUpdate(VersionComparator.INSTANCE.compare(latest, version) > 0)
                                .setLatest(latest);
                        Element markdownBody = box.selectFirst(".markdown-body");
                        about.setMarkdownBody(markdownBody.html());
                        String downloadUrl = StrFormatter.format("https://github.com/wushuo894/ani-rss/releases/download/v{}/ani-rss-jar-with-dependencies.jar", latest);
                        about.setDownloadUrl(downloadUrl);
                    });
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug(e.getMessage(), e);
        }
        return about;
    }

    public static void update(About about) {
        Boolean update = about.getUpdate();
        if (!update) {
            return;
        }
        File jar = getJar();
        if (!"jar".equals(FileUtil.extName(jar))) {
            throw new RuntimeException("非jar启动 不支持更新");
        }
        File file = new File(FileUtil.mainName(jar) + "_v" + about.getLatest() + ".jar");
        String downloadUrl = about.getDownloadUrl();
        HttpReq.get(downloadUrl, true)
                .then(res -> {
                    long contentLength = res.contentLength();
                    FileUtil.writeFromStream(res.bodyStream(), file, true);
                    if (contentLength != file.length()) {
                        throw new RuntimeException("下载出现问题");
                    }
                    ThreadUtil.execute(() -> {
                        FileUtil.rename(file, jar.getName(), true);
                        ThreadUtil.sleep(3000);
                        System.exit(0);
                    });
                });
    }

    public static File getJar() {
        return new File(System.getProperty("java.class.path").split(";")[0]);
    }

}
