package ani.rss.commons;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.Cleanup;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class MavenUtils {
    private static String version = "None";
    public static JarFile JAR_FILE = null;

    static {
        CurrentFile currentFile = getCurrentFile();
        try {
            if (currentFile.isFile()) {
                JAR_FILE = new JarFile(currentFile.getFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CurrentFile getCurrentFile() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        String splitStr = osInfo.isWindows() ? ";" : ":";
        String s = System.getProperty("java.class.path")
                .split(splitStr)[0];
        return new CurrentFile()
                .setFile(new File(s));
    }

    public static synchronized String getVersion() {
        if (!"None".equalsIgnoreCase(version)) {
            return version;
        }
        try {
            if (Objects.nonNull(JAR_FILE)) {
                String pomPath = "META-INF/maven/ani.rss/ani-rss-application/pom.xml";
                JarEntry jarEntry = JAR_FILE.getJarEntry(pomPath);
                if (Objects.isNull(jarEntry)) {
                    return "None";
                }
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
            Document document = XmlUtil.readXML(file);
            Element element = XmlUtil.getElement(document.getDocumentElement(), "version");
            if (Objects.nonNull(element)) {
                version = element.getTextContent();
            }
        }
        return version;
    }

    @Data
    @Accessors(chain = true)
    public static class CurrentFile implements Serializable {
        private File file;

        public String getName() {
            return file.getName();
        }

        public Boolean isDirectory() {
            return file.isDirectory();
        }

        public Boolean isFile() {
            return file.isFile();
        }

        public Boolean isExe() {
            if (isDirectory()) {
                return false;
            }

            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                return false;
            }

            return "exe".equalsIgnoreCase(extName);
        }

        public Boolean isJar() {
            if (isDirectory()) {
                return false;
            }

            String extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName)) {
                return false;
            }

            return "jar".equalsIgnoreCase(extName);
        }
    }

}
