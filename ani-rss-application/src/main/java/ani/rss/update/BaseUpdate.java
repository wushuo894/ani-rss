package ani.rss.update;

import ani.rss.commons.MavenUtils;
import ani.rss.entity.About;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;

import java.io.File;

@FunctionalInterface
public interface BaseUpdate {
    static BaseUpdate getInstance() {
        OsInfo osInfo = SystemUtil.getOsInfo();

        if (osInfo.isMac()) {
            return new MacUpdate();
        } else if (osInfo.isLinux()) {
            return new LinuxUpdate();
        } else if (osInfo.isWindows()) {
            return new WindowsUpdate();
        } else {
            String name = osInfo.getName();
            throw new IllegalArgumentException("不支持的系统 " + name);
        }
    }

    void update(File updateFile);

    default File downloadUpdateFile(About about) {
        File currentFile = MavenUtils.getCurrentFile();
        File file = new File(currentFile + ".tmp");

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

        return file;
    }
}
