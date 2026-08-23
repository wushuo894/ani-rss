package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.URLUtils;
import ani.rss.entity.Global;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

        File imgFile = getImgFile(imgUrl);
        if (!imgFile.exists()) {
            getImg(imgUrl, res -> {
                HttpReq.assertStatus(res);

                long length = res.contentLength();

                File tempFile = FileUtil.createTempFile();
                try (InputStream inputStream = res.bodyStream()) {
                    FileUtil.writeFromStream(inputStream, tempFile, true);
                } catch (Exception ignored) {
                }

                String url = HttpReq.getUrl(res);
                if (length > 0) {
                    Assert.isTrue(tempFile.length() == length, "图片下载失败 {}", url);
                }

                FileUtil.move(tempFile, imgFile, true);
            });
        }

        try {
            HttpServletResponse response = Global.RESPONSE.get();

            // 30 天
            long maxAge = 86400 * 30;
            setCacheControl(response, maxAge);

            String contentType = getContentType(URLUtil.getPath(imgUrl));

            response.setContentType(contentType);
            response.setContentLengthLong(imgFile.length());

            @Cleanup
            InputStream inputStream = FileUtil.getInputStream(imgFile);
            @Cleanup
            OutputStream outputStream = response.getOutputStream();
            IoUtil.copy(inputStream, outputStream);
        } catch (Exception ignored) {
        }
    }

    public File getImgFile(String imgUrl) {
        Assert.notBlank(imgUrl, "imgUrl 不能为空");

        String path = URLUtil.getPath(imgUrl);
        String extName = FileUtil.extName(path);

        String md5 = SecureUtil.md5(imgUrl);
        String filename = StrUtil.format("{}.{}", md5, extName);
        String dir = String.valueOf(md5.charAt(0));

        File configDir = ConfigUtil.getConfigDir();
        return Path.of(configDir.toString(), "img", dir, filename).toFile();
    }

    public void getImg(String url, Consumer<HttpResponse> consumer) {
        URI host = URLUtil.getHost(URLUtil.url(url));
        HttpReq.get(url)
                .then(res -> {
                    HttpConnection httpConnection = (HttpConnection) ReflectUtil.getFieldValue(res, "httpConnection");
                    URI host1 = URLUtil.getHost(httpConnection.getUrl());

                    // 处理mikan自动重定向的问题
                    if (host.toString().equals(host1.toString())) {
                        consumer.accept(res);
                        return;
                    }
                    String newUrl = url.replace(host.toString(), host1.toString());
                    getImg(newUrl, consumer);
                });
    }
}
