package ani.rss.web.action;

import ani.rss.commons.MavenUtils;
import ani.rss.web.annotation.Auth;
import ani.rss.web.util.ServerUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 网页处理
 */
@Auth(value = false)
@Slf4j
public class RootAction implements BaseAction {

    private static final String DEFAULT_INDEX_FILE_NAME = "index.html";

    private final String rootDir;

    private final List<String> indexFileNames;

    public RootAction() {
        this("dist", DEFAULT_INDEX_FILE_NAME);
    }

    public RootAction(String rootDir) {
        this(rootDir, DEFAULT_INDEX_FILE_NAME);
    }

    public RootAction(String rootDir, String... indexFileNames) {
        this.rootDir = rootDir;
        this.indexFileNames = CollUtil.toList(indexFileNames);
    }

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) {
        String path = request.getPath();
        String fileName = rootDir + path;

        Boolean ok = file(response, fileName, true);
        if (!ok) {
            BaseAction.writeNotFound();
        }
    }

    public Boolean file(HttpServerResponse response, String fileName, Boolean index) {
        if (!fileName.endsWith("/")) {
            String extName = FileUtil.extName(fileName);
            if (StrUtil.isBlank(extName)) {
                fileName += ".html";
            }
        }

        log.debug(fileName);
        try {
            EnumerationIter<URL> resourceIter = ResourceUtil.getResourceIter(fileName);
            for (URL url : resourceIter) {
                Optional<FileInfo> fileInfoOptional = toFileInfo(url, fileName);
                if (fileInfoOptional.isEmpty()) {
                    continue;
                }
                FileInfo fileInfo = fileInfoOptional.get();
                Boolean gzip = fileInfo.getGzip();
                if (gzip) {
                    response.setHeader(Header.CONTENT_ENCODING, "gzip");
                }

                @Cleanup
                InputStream inputStream = fileInfo.getInputStream();
                String contentType = getContentType(fileName);
                if (List.of("text/css", "application/x-javascript").contains(contentType)) {
                    response.setHeader(Header.CACHE_CONTROL, "private, max-age=86400");
                }

                Long fileSize = fileInfo.getFileSize();
                response.write(inputStream, Math.toIntExact(fileSize), contentType);
                return true;
            }
            if (!index) {
                return false;
            }
            for (String indexFileName : indexFileNames) {
                Boolean ok = file(response, fileName + indexFileName, false);
                if (ok) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public FileInfo toFileInfo(Boolean gzip, String fileName) {
        JarFile jarFile = MavenUtils.JAR_FILE;
        if (Objects.isNull(jarFile)) {
            return null;
        }

        if (gzip) {
            String gzipFileName = fileName + ".gz";
            JarEntry jarEntry = jarFile.getJarEntry(gzipFileName);
            if (Objects.nonNull(jarEntry) && !jarEntry.isDirectory()) {
                try {
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    return new FileInfo(fileName, (long) jarFile.size(), inputStream, true);
                } catch (IOException ignored) {
                }
            }
        }

        JarEntry jarEntry = jarFile.getJarEntry(fileName);
        if (Objects.isNull(jarEntry)) {
            return null;
        }
        if (!jarEntry.isDirectory()) {
            try {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                return new FileInfo(fileName, (long) jarFile.size(), inputStream, false);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public FileInfo toFileInfo(Boolean gzip, URL url) {
        String filepath = URLUtil.decode(url.getFile(), StandardCharsets.UTF_8);
        String gzipFilepath = filepath + ".gz";
        String fileName = FileUtil.getName(filepath);
        if (gzip) {
            File file = new File(gzipFilepath);
            if (file.exists() && file.isFile()) {
                try {
                    InputStream inputStream = FileUtil.getInputStream(file);
                    return new FileInfo(fileName, file.length(), inputStream, true);
                } catch (Exception ignored) {
                }
            }
        }

        File file = new File(filepath);
        if (file.exists() && file.isFile()) {
            try {
                InputStream inputStream = FileUtil.getInputStream(file);
                return new FileInfo(fileName, file.length(), inputStream, false);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Optional<FileInfo> toFileInfo(URL url, String fileName) {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        String acceptEncoding = request.getHeader(Header.ACCEPT_ENCODING);
        boolean gzip = StrUtil.contains(acceptEncoding, "gzip");

        String protocol = url.getProtocol();
        FileInfo fileInfo;
        if (protocol.equals("file")) {
            fileInfo = toFileInfo(gzip, url);
        } else {
            fileInfo = toFileInfo(gzip, fileName);
        }

        if (Objects.isNull(fileInfo)) {
            return Optional.empty();
        }
        return Optional.of(fileInfo);
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileInfo implements Serializable {
        private String fileName;
        private Long fileSize;
        private InputStream inputStream;
        private Boolean gzip;
    }

}
