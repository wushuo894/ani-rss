package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

@Slf4j
@Auth(type = AuthType.FORM)
@Path("/file")
public class FileAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String img = request.getParam("img");
        if (StrUtil.isNotBlank(img)) {
            img = new String(Base64.getDecoder().decode(img), StandardCharsets.UTF_8);
            response.setContentType(FileUtil.getMimeType(URLUtil.getPath(img)));

            File configDir = ConfigUtil.getConfigDir();

            File file = new File(URLUtil.getPath(img));
            configDir = new File(configDir + "/img/" + file.getParentFile().getName());
            FileUtil.mkdir(configDir);

            File imgFile = new File(configDir, file.getName());
            if (imgFile.exists()) {
                @Cleanup
                BufferedInputStream inputStream = FileUtil.getInputStream(imgFile);
                @Cleanup
                OutputStream out = response.getOut();
                IoUtil.copy(inputStream, out);
                return;
            }

            getImg(img, is -> {
                try {
                    FileUtil.writeFromStream(is, imgFile, true);
                    @Cleanup
                    BufferedInputStream inputStream = FileUtil.getInputStream(imgFile);
                    @Cleanup
                    OutputStream out = response.getOut();
                    IoUtil.copy(inputStream, out);
                } catch (Exception ignored) {
                }
            });
            return;
        }


        String filename = request.getParam("filename");
        if (StrUtil.isBlank(filename)) {
            response.send404("404 Not Found !");
            return;
        }
        String mimeType = FileUtil.getMimeType(filename);
        if (StrUtil.isBlank(mimeType)) {
            response.setContentType(mimeType);
        } else {
            response.setContentType(ContentType.OCTET_STREAM.getValue());
        }

        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        File configDir = ConfigUtil.getConfigDir();
        @Cleanup
        FileInputStream inputStream = IoUtil.toStream(new File(configDir + "/files/" + filename));
        @Cleanup
        OutputStream out = response.getOut();
        IoUtil.copy(inputStream, out);
    }

    public static void getImg(String url, Consumer<InputStream> consumer) {
        URI host = URLUtil.getHost(URLUtil.url(url));
        HttpReq.get(url, true)
                .then(res -> {
                    HttpConnection httpConnection = (HttpConnection) ReflectUtil.getFieldValue(res, "httpConnection");
                    URI host1 = URLUtil.getHost(httpConnection.getUrl());
                    if (host.toString().equals(host1.toString())) {
                        try {
                            @Cleanup
                            InputStream inputStream = res.bodyStream();
                            consumer.accept(inputStream);
                        } catch (Exception ignored) {
                        }
                        return;
                    }
                    String newUrl = url.replace(host.toString(), host1.toString());
                    getImg(newUrl, consumer);
                });
    }

}
