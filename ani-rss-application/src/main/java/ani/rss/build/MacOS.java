package ani.rss.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MacOS implements Runnable {
    @Override
    public void run() {
        String basedir = System.getProperty("basedir");

        File target = new File(basedir + "/target");
        Assert.isTrue(target.exists(), "target not exists");

        File macosZip = new File(target, "ani-rss-macos-main.zip");

        if (!macosZip.exists()) {
            String url = "https://github.com/wushuo894/ani-rss-macos/archive/refs/heads/main.zip";
            HttpUtil.downloadFile(url, macosZip);
        }

        ZipUtil.unzip(macosZip, macosZip.getParentFile());

        File jarFile = new File(target, "ani-rss.jar");

        Path path = Path.of(target.getPath(), "ani-rss-macos-main/ani-rss.app/Contents/MacOS/ani-rss.jar");

        FileUtil.copy(Paths.get(jarFile.getPath()), path, StandardCopyOption.REPLACE_EXISTING);

        ZipUtil.zip(new File(target, "ani-rss-macos.zip"), true, new File(target, "ani-rss-macos-main/ani-rss.app"));
    }
}
