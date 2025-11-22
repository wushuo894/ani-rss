package ani.rss.util.other;

import ani.rss.commons.*;
import ani.rss.entity.About;
import ani.rss.entity.Config;
import ani.rss.entity.Global;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
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

        About cacheAbout = CacheUtil.get(key);

        if (Objects.nonNull(cacheAbout)) {
            return cacheAbout;
        }

        String version = MavenUtil.getVersion();

        About about = new About()
                .setVersion(version)
                .setUpdate(false)
                .setAutoUpdate(false)
                .setLatest("")
                .setMarkdownBody("");
        try {
            HttpReq.get("https://github.com/wushuo894/ani-rss/releases/latest/download/info.json")
                    .timeout(3000)
                    .then(response -> {
                        int status = response.getStatus();
                        if (status == 404) {
                            return;
                        }
                        HttpReq.assertStatus(response);
                        JsonObject jsonObject = GsonStatic.fromJson(response.body(), JsonObject.class);
                        String latest = jsonObject.get("version").getAsString();

                        /*
                        禁止非跨小版本的更新
                        取前两位版本号判断是允许自动更新
                         */
                        String reg = "^[Vv]?(\\d+\\.\\d+)";
                        boolean autoUpdate = ReUtil.get(reg, latest, 1)
                                .equals(ReUtil.get(reg, version, 1));

                        about
                                .setAutoUpdate(autoUpdate)
                                .setUpdate(VersionComparator.INSTANCE.compare(latest, version) > 0)
                                .setLatest(latest)
                                .setMarkdownBody(jsonObject.get("markdown").getAsString());
                        String filename = "ani-rss-jar-with-dependencies.jar";
                        File jar = MavenUtil.getJar();
                        if ("exe".equals(FileUtil.extName(jar))) {
                            filename = "ani-rss-launcher.exe";
                        }
                        String downloadUrl = StrFormatter.format("https://github.com/wushuo894/ani-rss/releases/download/v{}/{}", latest, filename);
                        about.setDownloadUrl(downloadUrl);

                        try {
                            long time = jsonObject.get("time").getAsLong();
                            about.setDate(new Date(time));
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error("检测更新失败 {}", message);
            log.error(message, e);
        }
        // 缓存一分钟 防止加速网站风控
        CacheUtil.put(key, about, 1000 * 60);
        return about;
    }

    public static synchronized void update(About about) {
        Boolean update = about.getUpdate();
        if (!update) {
            return;
        }

        Assert.isTrue(MavenUtil.isJar(), "不支持更新");

        File jar = MavenUtil.getJar();
        String extName = StrUtil.blankToDefault(FileUtil.extName(jar), "");
        File file = new File(jar + ".tmp");

        FileUtil.del(file);
        String downloadUrl = about.getDownloadUrl();
        String downloadMd5 = HttpReq.get(downloadUrl + ".md5")
                .thenFunction(res -> {
                    int status = res.getStatus();
                    Assert.isTrue(res.isOk(), "Error: {}", status);
                    String md5 = res.body();
                    if (StrUtil.isNotBlank(md5)) {
                        md5 = md5.split("\n")[0].trim();
                    }
                    Assert.isTrue(Md5Util.isValidMD5(md5), "获取更新文件MD5失败");
                    return md5;
                });

        HttpReq.get(downloadUrl)
                .then(res -> {
                    HttpReq.assertStatus(res);
                    long contentLength = res.contentLength();
                    FileUtil.writeFromStream(res.bodyStream(), file, true);
                    Assert.isTrue(file.length() == contentLength, "下载出现问题");
                    Assert.isTrue(SecureUtil.md5(file).equals(downloadMd5), "更新文件的MD5不匹配");
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
                strings.add(FileUtil.getAbsolutePath(jar));
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
