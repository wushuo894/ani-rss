package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.URLUtils;
import ani.rss.entity.Global;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpConnection;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.Consumer;

@Slf4j
@RestController
public class ProxyImageController extends BaseController {

    @Auth
    @Operation(summary = "下载并缓存图片")
    @GetMapping("/proxyImage")
    public void proxyImage(@RequestParam("imgUrl") String imgUrl) {
        imgUrl = imgUrl.replace(" ", "+");
        imgUrl = Base64.decodeStr(imgUrl);
        URLUtils.verify(imgUrl);
        HttpServletResponse response = Global.RESPONSE.get();

        // 30 天
        long maxAge = 86400 * 30;
        setCacheControl(response, maxAge);

        String contentType = getContentType(URLUtil.getPath(imgUrl));

        File configDir = ConfigUtil.getConfigDir();

        File file = new File(URLUtil.getPath(imgUrl));
        configDir = Path.of(configDir.toString(), "img", file.getParentFile().getName()).toFile();
        FileUtil.mkdir(configDir);

        File imgFile = new File(configDir, file.getName());
        if (imgFile.exists()) {
            try {
                response.setContentType(contentType);
                response.setContentLengthLong(imgFile.length());

                @Cleanup
                InputStream inputStream = FileUtil.getInputStream(imgFile);
                @Cleanup
                OutputStream outputStream = response.getOutputStream();
                IoUtil.copy(inputStream, outputStream);
            } catch (Exception ignored) {
            }
            return;
        }

        getImg(imgUrl, is -> {
            try {
                FileUtil.writeFromStream(is, imgFile, true);

                response.setContentType(contentType);
                response.setContentLengthLong(imgFile.length());

                @Cleanup
                BufferedInputStream inputStream = FileUtil.getInputStream(imgFile);
                @Cleanup
                ServletOutputStream outputStream = response.getOutputStream();
                IoUtil.copy(inputStream, outputStream);
            } catch (Exception ignored) {
            }
        });
    }

    public void getImg(String url, Consumer<InputStream> consumer) {
        URI host = URLUtil.getHost(URLUtil.url(url));
        HttpReq.get(url)
                .then(res -> {
                    HttpConnection httpConnection = (HttpConnection) ReflectUtil.getFieldValue(res, "httpConnection");
                    URI host1 = URLUtil.getHost(httpConnection.getUrl());

                    // 处理mikan自动重定向的问题
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
