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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebUIService {
    private final GithubService githubService;

    public File getWebUIDir() {
        File configDir = ConfigUtil.getConfigDir();
        return new File(configDir, "webui");
    }

    @Nullable
    public WebUI getWebUI() {
        File webuiDir = getWebUIDir();
        File webuiJson = new File(webuiDir, "webui.json");

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
        Objects.requireNonNull(updateInfo, "无 WebUI 更新");

        Boolean update = updateInfo.getUpdate();
        Assert.isTrue(update, "无 WebUI 更新");

        log.info("更新 WebUI");

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

        File webuiDir = getWebUIDir();

        FileUtil.del(webuiDir);

        try {
            ZipUtil.unzip(tempFile, webuiDir);
        } finally {
            FileUtil.del(tempFile);
        }

        log.info("WebUI 更新完成");
    }

    public void upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        String extName = FileUtil.extName(originalFilename);
        Assert.isTrue("zip".equals(extName), "文件格式错误");

        log.info("上传 WebUI {}", originalFilename);

        File tempFile = FileUtil.createTempFile();
        try (InputStream inputStream = file.getInputStream()) {
            FileUtil.writeFromStream(inputStream, tempFile);
        } catch (Exception e) {
            throw new IllegalArgumentException("上传 WebUI 失败");
        }

        try (ZipFile zipFile = new ZipFile(tempFile)) {
            ZipEntry entry = zipFile.getEntry("webui.json");
            Objects.requireNonNull(entry);
        } catch (Exception e) {
            throw new IllegalArgumentException("上传 WebUI 失败");
        }

        File webuiDir = getWebUIDir();
        FileUtil.del(webuiDir);

        try {
            ZipUtil.unzip(tempFile, webuiDir);
        } finally {
            FileUtil.del(tempFile);
        }

        log.info("WebUI 上传完成");
    }

    public void delete() {
        File webuiDir = getWebUIDir();
        FileUtil.del(webuiDir);

        log.info("已删除 WebUI");
    }

}
