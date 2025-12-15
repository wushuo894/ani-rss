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
import lombok.Cleanup;
import lombok.Data;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
                response.write(inputStream, contentType);
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

    public InputStream toStream(AtomicBoolean gzip, String fileName) {
        JarFile jarFile = MavenUtils.JAR_FILE;
        if (Objects.isNull(jarFile)) {
            return null;
        }

        if (gzip.get()) {
            String gzipFileName = fileName + ".gz";
            JarEntry jarEntry = jarFile.getJarEntry(gzipFileName);
            if (Objects.nonNull(jarEntry) && !jarEntry.isDirectory()) {
                try {
                    return jarFile.getInputStream(jarEntry);
                } catch (IOException ignored) {
                }
            }
        }

        gzip.set(false);

        JarEntry jarEntry = jarFile.getJarEntry(fileName);
        if (Objects.isNull(jarEntry)) {
            return null;
        }
        if (!jarEntry.isDirectory()) {
            try {
                return jarFile.getInputStream(jarEntry);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public InputStream toStream(AtomicBoolean gzip, URL url) {
        String filepath = URLUtil.decode(url.getFile(), StandardCharsets.UTF_8);
        String gzipFilepath = filepath + ".gz";
        if (gzip.get()) {
            File file = new File(gzipFilepath);
            if (file.exists() && file.isFile()) {
                try {
                    return FileUtil.getInputStream(file);
                } catch (Exception ignored) {
                }
            }
        }

        gzip.set(false);

        File file = new File(filepath);
        if (file.exists() && file.isFile()) {
            try {
                return FileUtil.getInputStream(file);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Optional<FileInfo> toFileInfo(URL url, String fileName) {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        String acceptEncoding = request.getHeader(Header.ACCEPT_ENCODING);
        AtomicBoolean gzip = new AtomicBoolean(StrUtil.contains(acceptEncoding, "gzip"));

        String protocol = url.getProtocol();
        InputStream inputStream;
        if (protocol.equals("file")) {
            inputStream = toStream(gzip, url);
        } else {
            inputStream = toStream(gzip, fileName);
        }

        if (Objects.isNull(inputStream)) {
            return Optional.empty();
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setInputStream(inputStream)
                .setFileName(fileName)
                .setGzip(gzip.get());

        return Optional.of(fileInfo);
    }

    @Data
    @Accessors(chain = true)
    public static class FileInfo implements Serializable {
        private String fileName;
        private InputStream inputStream;
        private Boolean gzip;
    }

}
