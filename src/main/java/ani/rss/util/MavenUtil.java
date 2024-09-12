package ani.rss.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class MavenUtil {
    private static String version = "None";

    public static synchronized String getVersion() {
        if (!"None".equalsIgnoreCase(version)) {
            return version;
        }
        try {
            File jar = UpdateUtil.getJar();
            String extName = StrUtil.blankToDefault(FileUtil.extName(jar), "");
            if (jar.isFile() && List.of("exe", "jar").contains(extName)) {
                @Cleanup
                JarFile jarFile = new JarFile(jar);
                JarEntry jarEntry = jarFile.getJarEntry("META-INF/maven/ani.rss/ani-rss/pom.xml");
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                String s = IoUtil.readUtf8(inputStream);
                version = ReUtil.get("<version>(.*?)</version>", s, 1);
                return version;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        File file = new File("pom.xml");
        if (file.exists()) {
            String s = FileUtil.readUtf8String(file);
            version = ReUtil.get("<version>(.*?)</version>", s, 1);
        }
        return version;
    }
}
