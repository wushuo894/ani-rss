package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.codec.Base64;
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
import java.util.function.Consumer;

@Slf4j
@Auth(type = AuthType.FORM)
@Path("/file")
public class FileAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String img = request.getParam("img");
        if (StrUtil.isNotBlank(img)) {
            img = Base64.decodeStr(img);
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
        String s = request.getParam("config");
        if (StrUtil.isBlank(filename)) {
            response.send404("404 Not Found !");
            return;
        }
        if (Base64.isBase64(filename)) {
            filename = Base64.decodeStr(filename);
        }

        String mimeType = FileUtil.getMimeType(filename);
        if (StrUtil.isBlank(mimeType)) {
            response.setContentType(mimeType);
        } else {
            response.setContentType(ContentType.OCTET_STREAM.getValue());
        }

        FileInputStream inputStream = null;
        OutputStream out = null;
        try {
            out = response.getOut();
            if ("false".equals(s)) {
                System.out.println(new File(filename).exists());
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Type", "video/mp4");
                response.setHeader("Content-Disposition", "inline;filename=temp." + FileUtil.extName(filename));
                response.setHeader("Content-Length", String.valueOf(new File(filename).length()));
                inputStream = IoUtil.toStream(new File(filename));
            } else {
                response.setHeader("Content-Disposition", "attachment; filename=\"" + new File(filename).getName() + "\"");
                File configDir = ConfigUtil.getConfigDir();
                inputStream = IoUtil.toStream(new File(configDir + "/files/" + filename));
            }
            IoUtil.copy(inputStream, out, 40960);
        } finally {
            IoUtil.close(inputStream);
            IoUtil.close(out);
        }
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
