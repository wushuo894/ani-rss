package ani.rss.action;

import ani.rss.annotation.Auth;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
                if (url.getProtocol().equals("file")) {
                    File file = new File(URLUtil.decode(url.getFile(), StandardCharsets.UTF_8));
                    FileUtil.getMimeType(fileName);
                    if (file.isDirectory()) {
                        continue;
                    }
                    @Cleanup
                    InputStream inputStream = FileUtil.getInputStream(file);
                    response.write(inputStream, FileUtil.getMimeType(fileName));
                    return true;
                }
                JarFile jarFile = URLUtil.getJarFile(url);
                JarEntry jarEntry = jarFile.getJarEntry(fileName);
                if (jarEntry.isDirectory()) {
                    continue;
                }
                @Cleanup
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                String mimeType = FileUtil.getMimeType(fileName);
                if (StrUtil.isBlank(mimeType)) {
                    response.write(inputStream);
                    return true;
                }
                response.write(inputStream, FileUtil.getMimeType(fileName));
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
