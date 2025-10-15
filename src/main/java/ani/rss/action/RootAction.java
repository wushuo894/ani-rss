package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.util.other.MavenUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
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
            response.send404("404 Not Found !");
        }
    }

    public Boolean file(HttpServerResponse response, String fileName, Boolean index) {
        log.debug(fileName);
        try {
            EnumerationIter<URL> resourceIter = ResourceUtil.getResourceIter(fileName);
            for (URL url : resourceIter) {
                @Cleanup
                InputStream inputStream = toInputStream(url, fileName);
                if (Objects.isNull(inputStream)) {
                    continue;
                }
                String mimeType = FileUtil.getMimeType(fileName);
                mimeType = StrUtil.blankToDefault(mimeType, ContentType.OCTET_STREAM.getValue());

                if (List.of("text/css", "application/x-javascript").contains(mimeType)) {
                    response.setHeader(Header.CACHE_CONTROL, "private, max-age=86400");
                }
                response.write(inputStream, mimeType);
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

    public InputStream toInputStream(URL url, String fileName) throws IOException {
        String protocol = url.getProtocol();

        InputStream inputStream = null;
        if (protocol.equals("file")) {
            File file = new File(URLUtil.decode(url.getFile(), StandardCharsets.UTF_8));
            if (!file.isDirectory()) {
                inputStream = FileUtil.getInputStream(file);
            }
        } else {
            JarFile jarFile = MavenUtil.JAR_FILE;
            if (Objects.isNull(jarFile)) {
                return null;
            }
            JarEntry jarEntry = jarFile.getJarEntry(fileName);
            if (Objects.isNull(jarEntry)) {
                return null;
            }
            if (!jarEntry.isDirectory()) {
                inputStream = jarFile.getInputStream(jarEntry);
            }
        }
        return inputStream;
    }

}
