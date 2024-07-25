package ani.rss.util;

import ani.rss.Main;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.log.Log;
import lombok.Cleanup;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MavenUtil {
    private static final Log log = Log.get(MavenUtil.class);

    public static String getVersion() {
        try {
            String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            File jar = new File(path);
            if (jar.isFile() && "jar".equals(FileUtil.extName(jar))) {
                @Cleanup
                JarFile jarFile = new JarFile(jar);
                JarEntry jarEntry = jarFile.getJarEntry("META-INF/maven/ani.rss/ani-rss/pom.xml");
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                String s = IoUtil.readUtf8(inputStream);
                return ReUtil.get("<version>(.*?)</version>", s, 1);
            }
        } catch (Exception e) {
            log.error(e);
        }
        File file = new File("pom.xml");
        if (file.exists()) {
            String s = FileUtil.readUtf8String(file);
            return ReUtil.get("<version>(.*?)</version>", s, 1);
        }
        return "None";
    }
}
