package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Global;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import ani.rss.entity.web.Result;
import ani.rss.service.MikanService;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.SourceSelectorUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpConnection;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

@RestController
public class MikanController extends BaseController {

    @Resource
    private MikanService mikanService;

    @Auth
    @Operation(summary = "获取Mikan番剧列表")
    @PostMapping("/mikan")
    public Result<Mikan> mikan(@RequestParam("text") String text, @RequestBody Mikan.Season season) {
        Mikan list = mikanService.list(text, season);
        return Result.success(list);
    }

    @Auth
    @Operation(summary = "获取Mikan番剧的字幕组列表")
    @PostMapping("/mikanGroup")
    public Result<List<Mikan.Group>> mikanGroup(@RequestParam("url") String url) {
        List<Mikan.Group> groups = mikanService.getGroups(url);

        for (Mikan.Group group : groups) {
            List<TorrentsInfo> items = group.getItems();
            SourceSelectorUtil.SelectorData<Mikan.RegexItem> selectorData = SourceSelectorUtil.build(
                    items.stream().map(TorrentsInfo::getName).toList(),
                    Mikan.RegexItem::new
            );
            group.setRegexList(selectorData.regexList())
                    .setTags(selectorData.tags());
        }
        return Result.success(groups);
    }

    @Auth
    @Operation(summary = "获取Mikan封面")
    @GetMapping("/mikanCover")
    public void mikanCover(@RequestParam("img") String img) {
        if (Base64.isBase64(img)) {
            img = img.replace(" ", "+");
            img = Base64.decodeStr(img);
        }
        HttpServletResponse response = Global.RESPONSE.get();

        // 30 天
        long maxAge = 86400 * 30;
        setCacheControl(response, maxAge);

        String contentType = getContentType(URLUtil.getPath(img));

        File configDir = ConfigUtil.getConfigDir();

        File file = new File(URLUtil.getPath(img));
        configDir = new File(configDir + "/img/" + file.getParentFile().getName());
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

        getImg(img, is -> {
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
