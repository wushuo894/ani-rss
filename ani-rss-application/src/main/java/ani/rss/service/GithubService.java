package ani.rss.service;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.entity.Github;
import ani.rss.entity.UpdateInfo;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GithubService {

    public Optional<Github.Release> getLatest(String owner, String repo) {
        Config config = ConfigUtil.CONFIG;
        String githubToken = config.getGithubToken();


        String latestUrl = "https://api.github.com/repos/{}/{}/releases/latest";

        latestUrl = StrUtil.format(latestUrl, owner, repo);

        HttpRequest request = HttpReq.get(latestUrl)
                .timeout(3000);

        if (StrUtil.isNotBlank(githubToken)) {
            request.header(Header.AUTHORIZATION, "Bearer " + githubToken);
        }

        return request.thenFunction(res -> {
            int status = res.getStatus();
            if (status == 404) {
                return Optional.empty();
            }
            HttpReq.assertStatus(res);
            Github.Release release = GsonStatic.fromJson(res.body(), Github.Release.class);
            return Optional.of(release);
        });
    }

    public UpdateInfo getUpdateInfo(String owner, String repo, String filename, String currentVersion) {
        UpdateInfo updateInfo = new UpdateInfo();
        updateInfo
                .setUpdate(false)
                .setAutoUpdate(false)
                .setLatest("")
                .setDownloadUrl("")
                .setSha256("")
                .setMarkdownBody("")
                .setSize(0L)
                .setFormatSize("0 MiB");

        Optional<Github.Release> releaseOpt = getLatest(owner, repo);
        if (releaseOpt.isEmpty()) {
            return updateInfo;
        }

        Github.Release release = releaseOpt.get();

        String message = release.getMessage();
        if (StrUtil.isNotBlank(message)) {
            log.error(message);
            return updateInfo;
        }

        String latest = release.getTagName().replace("v", "");

            /*
            禁止非跨小版本的更新
            取前两位版本号判断是允许自动更新
            */
        String reg = "^[Vv]?(\\d+\\.\\d+)";
        boolean autoUpdate = ReUtil.get(reg, latest, 1)
                .equals(ReUtil.get(reg, currentVersion, 1));

        boolean update = VersionComparator.INSTANCE.compare(latest, currentVersion) > 0;

        updateInfo
                .setDate(release.getPublishedAt())
                .setAutoUpdate(autoUpdate)
                .setUpdate(false)
                .setLatest(latest)
                .setMarkdownBody(release.getBody());


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

            updateInfo
                    .setUpdate(update)
                    .setDownloadUrl(asset.getBrowserDownloadUrl())
                    .setSha256(sha256)
                    .setSize(size)
                    .setFormatSize(formatSize);
        }

        return updateInfo;
    }

}
