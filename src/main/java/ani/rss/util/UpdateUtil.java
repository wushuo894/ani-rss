package ani.rss.util;

import ani.rss.entity.About;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.util.List;

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
                        String filename = "ani-rss-jar-with-dependencies.jar";
                        File jar = getJar();
                        if ("exe".equals(FileUtil.extName(jar))) {
                            filename = "ani-rss-launcher.exe";
                        }
                        String downloadUrl = StrFormatter.format("https://github.com/wushuo894/ani-rss/releases/download/v{}/{}", latest, filename);
                        about.setDownloadUrl(downloadUrl);
                    });
        } catch (Exception e) {
            log.error("检测更新失败 {}", e.getMessage());
            log.debug(e.getMessage(), e);
        }
        return about;
    }

    public static void update(About about) {
        Boolean update = about.getUpdate();
        String latest = about.getLatest();
        if (!update) {
            return;
        }
        File jar = getJar();
        String extName = StrUtil.blankToDefault(FileUtil.extName(jar), "");
        String mainName = FileUtil.mainName(jar);
        if (!List.of("jar", "exe").contains(extName)) {
            throw new RuntimeException("不支持更新");
        }
        if ("exe".equals(extName)) {
            mainName = "ani-rss-launcher";
        }
        File file = new File(StrFormatter.format("{}_v{}.{}", mainName, latest, extName));
        FileUtil.del(file);
        String downloadUrl = about.getDownloadUrl();
        HttpReq.get(downloadUrl, true)
                .then(res -> {
                    long contentLength = res.contentLength();
                    FileUtil.writeFromStream(res.bodyStream(), file, true);
                    if (contentLength != file.length()) {
                        log.error("下载出现问题");
                        return;
                    }
                    ThreadUtil.execute(() -> {
                        ThreadUtil.sleep(3000);
                        if ("exe".equals(extName)) {
                            ServerUtil.stop();
                            RuntimeUtil.exec(file.getName());
                            System.exit(0);
                            return;
                        }
                        FileUtil.rename(file, jar.getName(), true);
                        System.exit(0);
                    });
                });
    }

    public static File getJar() {
        return new File(System.getProperty("java.class.path").split(";")[0]);
    }

}
