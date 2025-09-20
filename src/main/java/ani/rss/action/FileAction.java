package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.ConfigUtil;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.function.Consumer;

/**
 * 文件
 */
@Slf4j
@Auth
@Path("/file")
public class FileAction implements BaseAction {

    public static void getImg(String url, Consumer<InputStream> consumer) {
        URI host = URLUtil.getHost(URLUtil.url(url));
        HttpReq.get(url)
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

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String img = request.getParam("img");
        if (StrUtil.isNotBlank(img)) {
            response.setHeader(Header.CACHE_CONTROL, "private, max-age=86400");
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

        if (StrUtil.isBlank(filename)) {
            response.send404("404 Not Found !");
            return;
        }
        if (Base64.isBase64(filename)) {
            filename = Base64.decodeStr(filename);
        }

        File file = new File(filename);
        if (!file.exists()) {
            File configDir = ConfigUtil.getConfigDir();
            file = new File(configDir + "/files/" + filename);
            if (!file.exists()) {
                response.send404("404 Not Found !");
                return;
            }
        }

        boolean hasRange = false;
        long start = 0;
        long end = file.length();

        String mimeType = FileUtil.getMimeType(filename);

        response.setHeader("Content-Disposition", StrFormatter.format("inline; filename=\"{}\"", URLUtil.encode(new File(filename).getName())));
        if (StrUtil.isBlank(mimeType)) {
            response.setContentType(ContentType.OCTET_STREAM.getValue());
        } else if (mimeType.startsWith("video/")) {
            String extName = FileUtil.extName(filename);
            response.setHeader("Content-Type", "video/" + extName);
            response.setHeader("Accept-Ranges", "bytes");
            String rangeHeader = request.getHeader("Range");
            long fileLength = file.length();
            if (StrUtil.isNotBlank(rangeHeader) && rangeHeader.startsWith("bytes=")) {
                String[] range = rangeHeader.substring(6).split("-");
                if (range.length > 0) {
                    start = Long.parseLong(range[0]);
                }
                if (range.length > 1) {
                    end = Long.parseLong(range[1]);
                }
                long contentLength = end - start;
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(contentLength));
                hasRange = true;
            } else {
                response.setHeader("Content-Length", String.valueOf(fileLength));
            }
        } else {
            response.setHeader(Header.CACHE_CONTROL, "private, max-age=86400");
            response.setContentType(mimeType);
        }

        try {
            if (hasRange) {
                response.send(206);
                @Cleanup
                OutputStream out = response.getOut();
                @Cleanup
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                randomAccessFile.seek(start);
                FileChannel channel = randomAccessFile.getChannel();
                @Cleanup
                InputStream inputStream = Channels.newInputStream(channel);
                IoUtil.copy(inputStream, out, 40960, end - start, null);
            } else {
                @Cleanup
                OutputStream out = response.getOut();
                @Cleanup
                InputStream inputStream = FileUtil.getInputStream(file);
                IoUtil.copy(inputStream, out, 40960);
            }
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.debug(message, e);
        }
    }

}
