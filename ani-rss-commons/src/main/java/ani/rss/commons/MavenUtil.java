package ani.rss.commons;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class MavenUtil {
    private static String version = "None";
    public static JarFile JAR_FILE = null;

    static {
        File jar = getJar();
        try {
            if (isJar()) {
                JAR_FILE = new JarFile(jar);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getJar() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        String splitStr = osInfo.isWindows() ? ";" : ":";
        String s = System.getProperty("java.class.path")
                .split(splitStr)[0];
        return new File(s);
    }

    public static Boolean isJar() {
        File jar = getJar();

        if (jar.isDirectory()) {
            return false;
        }

        String extName = FileUtil.extName(jar);
        if (StrUtil.isBlank(extName)) {
            return false;
        }

        return List.of("exe", "jar").contains(extName);
    }

    public static synchronized String getVersion() {
        if (!"None".equalsIgnoreCase(version)) {
            return version;
        }
        try {
            if (Objects.nonNull(JAR_FILE)) {
                String pomPath = "META-INF/maven/ani.rss/ani-rss-application/pom.xml";
                JarEntry jarEntry = JAR_FILE.getJarEntry(pomPath);
                @Cleanup
                InputStream inputStream = JAR_FILE.getInputStream(jarEntry);
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
