package ani.rss.service;

import ani.rss.entity.UpdateInfo;
import ani.rss.entity.WebUI;
import ani.rss.handle.JsonReader;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebUIService {
    private final GithubService githubService;

    @Nullable
    public WebUI getWebUI() {
        File configDir = ConfigUtil.getConfigDir();
        File webuiJson = new File(configDir, "webui/webui.json");

        if (FileUtil.exist(webuiJson)) {
            return JsonReader.getInstance(webuiJson)
                    .toObject(WebUI.class);
        }

        return null;
    }

    @Nullable
    public UpdateInfo getUpdate() {
        WebUI webUI = getWebUI();
        if (Objects.isNull(webUI)) {
            return null;
        }

        String owner = webUI.getOwner();
        String repo = webUI.getRepo();
        String version = webUI.getVersion();
        String filename = webUI.getFilename();

        return githubService.getUpdateInfo(owner, repo, filename, version);
    }

    public void update() {
        UpdateInfo updateInfo = getUpdate();
        if (Objects.isNull(updateInfo)) {
            return;
        }

        Boolean update = updateInfo.getUpdate();
        Assert.isTrue(update, "WebUI 无更新");

        File tempFile = FileUtil.createTempFile();

        String downloadUrl = updateInfo.getDownloadUrl();
        String sha256 = updateInfo.getSha256();
        long size = updateInfo.getSize();

        HttpReq.get(downloadUrl)
                .then(res -> {
                    HttpReq.assertStatus(res);
                    FileUtil.writeFromStream(res.bodyStream(), tempFile, true);
                    Assert.isTrue(tempFile.length() == size, "WebUI 下载出现问题");
                    Assert.isTrue(SecureUtil.sha256(tempFile).equals(sha256), "WebUI 更新文件的 sha256 不匹配");
                });

        File webuiDir = new File(ConfigUtil.getConfigDir(), "webui");

        FileUtil.del(webuiDir);

        ZipUtil.unzip(tempFile, webuiDir);
    }


}
