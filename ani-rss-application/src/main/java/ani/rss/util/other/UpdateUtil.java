package ani.rss.util.other;

import ani.rss.commons.*;
import ani.rss.entity.About;
import ani.rss.entity.Config;
import ani.rss.entity.Github;
import ani.rss.entity.Global;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class UpdateUtil {

    public static synchronized About about() {
        Config config = ConfigUtil.CONFIG;
        String github = config.getGithub();
        Boolean customGithub = config.getCustomGithub();
        String customGithubUrl = config.getCustomGithubUrl();

        String key = StrFormatter.format("github#{} {} {}", github, customGithub, customGithubUrl);

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
            HttpReq.get("https://api.github.com/repos/wushuo894/ani-rss/releases/latest")
                    .timeout(3000)
                    .then(response -> {
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

                        String latest = release.getTagName().replace("v","");

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

                        String filename = "ani-rss.jar";
                        File jar = MavenUtils.getJar();
                        if ("exe".equals(FileUtil.extName(jar))) {
                            filename = "ani-rss.exe";
                        }

                        List<Github.Assets> assets = release.getAssets();
                        for (Github.Assets asset : assets) {
                            String name = asset.getName();
                            if (!filename.equals(name)) {
                                continue;
                            }

                            String sha256 = asset.getDigest()
                                    .replace("sha256:", "");

                            about.setDownloadUrl(asset.getBrowserDownloadUrl())
                                    .setSha256(sha256)
                                    .setSize(asset.getSize());
                        }
                    });
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error("检测更新失败 {}", message);
            log.error(message, e);
        }
        // 缓存一分钟 防止加速网站风控
        CacheUtils.put(key, about, 1000 * 60);
        return about;
    }

    public static synchronized void update(About about) {
        Boolean update = about.getUpdate();
        if (!update) {
            return;
        }

        Assert.isTrue(MavenUtils.isJar(), "不支持更新");

        File jar = MavenUtils.getJar();
        String extName = StrUtil.blankToDefault(FileUtil.extName(jar), "");
        File file = new File(jar + ".tmp");

        FileUtil.del(file);
        String downloadUrl = about.getDownloadUrl();
        String sha256 = about.getSha256();
        long size = about.getSize();

        HttpReq.get(downloadUrl)
                .then(res -> {
                    HttpReq.assertStatus(res);
                    FileUtil.writeFromStream(res.bodyStream(), file, true);
                    Assert.isTrue(file.length() == size, "下载出现问题");
                    Assert.isTrue(SecureUtil.sha256(file).equals(sha256), "更新文件的 sha256 不匹配");
                });

        ThreadUtil.execute(() -> {
            if ("jar".equals(extName)) {
                FileUtil.rename(file, jar.getName(), true);
                System.exit(0);
                return;
            }
            String filename = "ani-rss-update.exe";
            File updateExe = new File(file.getParent() + "/" + filename);
            FileUtil.del(updateExe);
            try (InputStream stream = ResourceUtil.getStream(filename)) {
                FileUtil.writeFromStream(stream, updateExe, true);
                List<String> strings = new ArrayList<>();
                strings.add(updateExe.toString());
                strings.add(FileUtils.getAbsolutePath(jar));
                strings.addAll(Global.ARGS);
                String[] array = ArrayUtil.toArray(strings, String.class);
                RuntimeUtil.exec(array);
                System.exit(0);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

}
