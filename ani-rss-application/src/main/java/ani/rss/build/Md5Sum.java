package ani.rss.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Objects;

@Slf4j
public class Md5Sum implements Runnable {

    @Override
    public void run() {
        String basedir = System.getProperty("basedir");
        File target = new File(basedir + "/target");
        Assert.isTrue(target.exists(), "target not exists");

        File[] files = target.listFiles();
        Assert.notEmpty(files, "files not found");
        Objects.requireNonNull(files, "files not found");

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                continue;
            }
            if ("md5".equals(extName)) {
                continue;
            }
            generate(file);
        }
    }

    private void generate(File file) {
        File md5File = new File(file + ".md5");
        if (md5File.exists()) {
            return;
        }

        String md5 = SecureUtil.md5(file);
        FileUtil.writeUtf8String(md5, md5File);

        log.info("md5 {} {}", file, md5);
    }
}
