package ani.rss.controller;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Global;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConnection;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.function.Consumer;

@Slf4j
@RestController
public class FileController extends BaseController {
    @GetMapping("/file")
    public void file() {
        HttpServletRequest request = Global.REQUEST.get();
        String img = request.getParameter("img");
        if (StrUtil.isNotBlank(img)) {
            if (Base64.isBase64(img)) {
                img = Base64.decodeStr(img);
            }
            doImg(img);
            return;
        }

        String filename = request.getParameter("filename");

        if (StrUtil.isBlank(filename)) {
            writeNotFound();
            return;
        }

        if (Base64.isBase64(filename)) {
            filename = Base64.decodeStr(filename);
        }

        doFile(filename);
    }

    /**
     * 处理图片文件
     *
     * @param img 图片名
     */
    public void doImg(String img) {
        HttpServletResponse response = Global.RESPONSE.get();

        // 30 天
        long maxAge = 86400 * 30;

        response.setHeader(Header.CACHE_CONTROL.toString(), "private, max-age=" + maxAge);

        String contentType = getContentType(URLUtil.getPath(img));

        File configDir = ConfigUtil.getConfigDir();

        File file = new File(URLUtil.getPath(img));
        configDir = new File(configDir + "/img/" + file.getParentFile().getName());
        FileUtil.mkdir(configDir);

        File imgFile = new File(configDir, file.getName());
        if (imgFile.exists()) {
            try {
                response.setContentType(contentType);
                @Cleanup
                InputStream inputStream = FileUtil.getInputStream(imgFile);
                @Cleanup
                OutputStream outputStream = response.getOutputStream();
                IoUtil.copy(inputStream, outputStream);
            } catch (Exception ignored) {
            }
            return;
        }

        getImg(img, is -> {
            try {
                response.setContentType(contentType);
                FileUtil.writeFromStream(is, imgFile, true);
                @Cleanup
                BufferedInputStream inputStream = FileUtil.getInputStream(imgFile);
                @Cleanup
                ServletOutputStream outputStream = response.getOutputStream();
                IoUtil.copy(inputStream, outputStream);
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
        HttpServletRequest request = Global.REQUEST.get();
        HttpServletResponse response = Global.RESPONSE.get();

        File file = new File(filename);
        if (!file.exists()) {
            File configDir = ConfigUtil.getConfigDir();
            file = new File(configDir + "/files/" + filename);
            if (!file.exists()) {
                writeNotFound();
                return;
            }
        }

        boolean hasRange = false;
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        String contentType = getContentType(file.getName());

        response.setHeader(Header.CONTENT_DISPOSITION.toString(), StrFormatter.format("inline; filename=\"{}\"", URLUtil.encode(file.getName())));
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

            response.setHeader(Header.CACHE_CONTROL.toString(), "private, max-age=" + maxAge);
            response.setContentType(contentType);
        }

        try {
            if (hasRange) {
                long length = end - start;
                response.setStatus(206);
                @Cleanup
                OutputStream out = response.getOutputStream();
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
                @Cleanup
                OutputStream outputStream = response.getOutputStream();
                IoUtil.copy(inputStream, outputStream);
            }
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.debug(message, e);
        }
    }


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
}
