package ani.rss.util.other;

import ani.rss.commons.*;
import ani.rss.entity.About;
import ani.rss.entity.Config;
import ani.rss.entity.Github;
import ani.rss.update.BaseUpdate;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Slf4j
public class UpdateUtil {

    public static synchronized About about() {
        Config config = ConfigUtil.CONFIG;
        String key = "github#releases-latest";

        About cacheAbout = CacheUtils.get(key);

        if (Objects.nonNull(cacheAbout)) {
            return cacheAbout;
        }

        String version = MavenUtils.getVersion();

        About about = new About()
                .setVersion(version)
                .setUpdate(false)
                .setAutoUpdate(false)
                .setLatest("")
                .setMarkdownBody("");
        try {
            HttpRequest request = HttpReq.get("https://api.github.com/repos/wushuo894/ani-rss/releases/latest")
                    .timeout(3000);

            String githubToken = config.getGithubToken();
            if (StrUtil.isNotBlank(githubToken)) {
                request.header(Header.AUTHORIZATION, "Bearer " + githubToken);
            }

            request.then(response -> {
                int status = response.getStatus();
                if (status == 404) {
                    return;
                }
                HttpReq.assertStatus(response);

                Github.Release release = GsonStatic.fromJson(response.body(), Github.Release.class);

                String message = release.getMessage();
                if (StrUtil.isNotBlank(message)) {
                    log.error(message);
                    return;
                }

                String latest = release.getTagName().replace("v", "");

                /*
                禁止非跨小版本的更新
                取前两位版本号判断是允许自动更新
                */
                String reg = "^[Vv]?(\\d+\\.\\d+)";
                boolean autoUpdate = ReUtil.get(reg, latest, 1)
                        .equals(ReUtil.get(reg, version, 1));

                about
                        .setDate(release.getPublishedAt())
                        .setAutoUpdate(autoUpdate)
                        .setUpdate(VersionComparator.INSTANCE.compare(latest, version) > 0)
                        .setLatest(latest)
                        .setMarkdownBody(release.getBody());

                MavenUtils.CurrentFile currentFile = MavenUtils.getCurrentFile();

                String filename = currentFile.isJar() ? "ani-rss.jar" : "ani-rss.exe";

                List<Github.Assets> assets = release.getAssets();
                for (Github.Assets asset : assets) {
                    String name = asset.getName();
                    if (!filename.equals(name)) {
                        continue;
                    }

                    Long size = asset.getSize();
                    String formatSize = FileUtils.formatSize(size, true);

                    String sha256 = asset.getDigest()
                            .replace("sha256:", "");

                    about.setDownloadUrl(asset.getBrowserDownloadUrl())
                            .setSha256(sha256)
                            .setSize(size)
                            .setFormatSize(formatSize);
                }
            });
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error("检测更新失败 {}", message);
            log.error(message, e);
        }
        // 缓存一分钟
        CacheUtils.put(key, about, 1000 * 60);
        return about;
    }

    public static synchronized void update(About about) {
        Boolean update = about.getUpdate();
        if (!update) {
            return;
        }

        MavenUtils.CurrentFile currentFile = MavenUtils.getCurrentFile();

        Assert.isTrue(currentFile.isFile(), "不支持更新");

        BaseUpdate baseUpdate = BaseUpdate.getInstance();

        File updateFile = baseUpdate.downloadUpdateFile(about);

        ThreadUtil.execute(() -> {
            try {
                baseUpdate.update(updateFile);
            } catch (Exception e) {
                log.error("更新时遇到错误: {}", e.getMessage(), e);
            }
        });
    }

}
