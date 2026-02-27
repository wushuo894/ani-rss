package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Global;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

@Slf4j
@RestController
public class FileController extends BaseController {

    @Auth
    @Operation(summary = "获取文件")
    @GetMapping("/file")
    public void file(@RequestParam("filename") String filename) {
        if (Base64.isBase64(filename)) {
            filename = filename.replace(" ", "+");
            filename = Base64.decodeStr(filename);
        }

        doFile(filename);
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
}
