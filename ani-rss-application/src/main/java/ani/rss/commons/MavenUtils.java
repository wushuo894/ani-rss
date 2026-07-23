package ani.rss.commons;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

@Slf4j
public class MavenUtils {
    private static String version;

    public static CurrentFile getCurrentFile() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        String splitStr = osInfo.isWindows() ? ";" : ":";
        String s = System.getProperty("java.class.path")
                .split(splitStr)[0];
        return new CurrentFile()
                .setFile(new File(s));
    }

    public static String getVersion() {
        if (Objects.nonNull(version)) {
            return version;
        }
        File file = new File("pom.xml");
        if (file.exists()) {
            Document document = XmlUtil.readXML(file);
            Element element = XmlUtil.getElement(document.getDocumentElement(), "version");
            if (Objects.nonNull(element)) {
                version = element.getTextContent();
                return version;
            }
        }
        BuildProperties buildProperties = SpringUtil.getBean(BuildProperties.class);
        version = buildProperties.getVersion();
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
