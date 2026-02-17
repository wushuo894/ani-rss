package ani.rss.action;

import ani.rss.commons.ExceptionUtils;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.web.util.ServerUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
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

    public void getImg(String url, Consumer<InputStream> consumer) {
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

    /**
     * 处理图片文件
     *
     * @param img 图片名
     */
    public void doImg(String img) {
        HttpServerResponse response = ServerUtil.RESPONSE.get();

        // 30 天
        long maxAge = 86400 * 30;

        response.setHeader(Header.CACHE_CONTROL, "private, max-age=" + maxAge);

        String contentType = getContentType(URLUtil.getPath(img));

        File configDir = ConfigUtil.getConfigDir();

        File file = new File(URLUtil.getPath(img));
        configDir = new File(configDir + "/img/" + file.getParentFile().getName());
        FileUtil.mkdir(configDir);

        File imgFile = new File(configDir, file.getName());
        if (imgFile.exists()) {
            try {
                @Cleanup
                InputStream inputStream = FileUtil.getInputStream(imgFile);
                response.write(inputStream, (int) imgFile.length(), contentType);
            } catch (Exception ignored) {
            }
            return;
        }

        getImg(img, is -> {
            try {
                FileUtil.writeFromStream(is, imgFile, true);
                @Cleanup
                BufferedInputStream inputStream = FileUtil.getInputStream(imgFile);
                response.write(inputStream, (int) imgFile.length(), contentType);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * 处理文件
     *
     * @param filename 文件名
     */
    private void doFile(String filename) {
        HttpServerRequest request = ServerUtil.REQUEST.get();
        HttpServerResponse response = ServerUtil.RESPONSE.get();

        File file = new File(filename);
        if (!file.exists()) {
            File configDir = ConfigUtil.getConfigDir();
            file = new File(configDir + "/files/" + filename);
            if (!file.exists()) {
                BaseAction.writeNotFound();
                return;
            }
        }

        boolean hasRange = false;
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        String contentType = getContentType(file.getName());

        response.setHeader(Header.CONTENT_DISPOSITION, StrFormatter.format("inline; filename=\"{}\"", URLUtil.encode(file.getName())));
        if (contentType.startsWith("video/")) {
            response.setContentType(contentType);
            response.setHeader("Accept-Ranges", "bytes");
            String rangeHeader = request.getHeader("Range");
            if (StrUtil.isNotBlank(rangeHeader) && rangeHeader.startsWith("bytes=")) {
                String[] range = rangeHeader.substring(6).split("-");
                if (range.length > 0) {
                    start = Long.parseLong(range[0]);
                }
                if (range.length > 1) {
                    end = Long.parseLong(range[1]);
                } else {
                    long maxEnd = start + (1024 * 1024 * 10);
                    end = Math.min(end, maxEnd);
                }
            }
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            hasRange = true;
        } else {
            long maxAge = 0;

            // 小于或者等于 3M 缓存
            if (fileLength <= 1024 * 1024 * 3) {
                // 30 天
                maxAge = 86400 * 30;
            }

            response.setHeader(Header.CACHE_CONTROL, "private, max-age=" + maxAge);
            response.setContentType(contentType);
        }

        try {
            if (hasRange) {
                long length = end - start;
                response.send(206, length);
                @Cleanup
                OutputStream out = response.getOut();
                @Cleanup
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                randomAccessFile.seek(start);
                @Cleanup
                FileChannel channel = randomAccessFile.getChannel();
                @Cleanup
                InputStream inputStream = Channels.newInputStream(channel);
                IoUtil.copy(inputStream, out, 40960, length, null);
            } else {
                @Cleanup
                InputStream inputStream = FileUtil.getInputStream(file);
                response.write(inputStream, (int) fileLength);
            }
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.debug(message, e);
        }
    }

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String img = request.getParam("img");
        if (StrUtil.isNotBlank(img)) {
            if (Base64.isBase64(img)) {
                img = Base64.decodeStr(img);
            }
            doImg(img);
            return;
        }

        String filename = request.getParam("filename");

        if (StrUtil.isBlank(filename)) {
            BaseAction.writeNotFound();
            return;
        }

        if (Base64.isBase64(filename)) {
            filename = Base64.decodeStr(filename);
        }

        doFile(filename);
    }

}
