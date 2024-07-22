package ani.rss.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.EnumerationIter;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import lombok.Cleanup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 网页处理
 */
public class RootAction implements Action {

    public static final String DEFAULT_INDEX_FILE_NAME = "index.html";

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
        final String path = request.getPath();
        String fileName = rootDir + path;

        Boolean ok = file(response, fileName, true);
        if (!ok) {
            response.send404("404 Not Found !");
        }
    }

    public Boolean file(HttpServerResponse response, String fileName, Boolean index) {
        System.out.println(fileName);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}