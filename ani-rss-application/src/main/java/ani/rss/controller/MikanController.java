package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Global;
import ani.rss.entity.Mikan;
import ani.rss.entity.Result;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.MikanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpConnection;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@RestController
public class MikanController extends BaseController {

    @Auth
    @Operation(summary = "获取Mikan番剧列表")
    @PostMapping("/mikan")
    public Result<Mikan> mikan(@RequestParam("text") String text, @RequestBody Mikan.Season season) {
        Mikan list = MikanUtil.list(text, season);
        return Result.success(list);
    }

    @Auth
    @Operation(summary = "获取Mikan番剧的字幕组列表")
    @PostMapping("/mikanGroup")
    public Result<List<Mikan.Group>> mikanGroup(@RequestParam("url") String url) {
        List<Mikan.Group> groups = MikanUtil.getGroups(url);

        List<String> regexItemList = List.of(
                "1920[Xx]1080", "3840[Xx]2160", "1080[Pp]", "4[Kk]", "720[Pp]",
                "繁", "简", "日",
                "cht|Cht|CHT", "chs|Chs|CHS", "hevc|Hevc|HEVC",
                "10bit|10Bit|10BIT", "h265|H265", "h264|H264",
                "内嵌", "内封", "外挂",
                "mp4|MP4", "mkv|MKV"
        );

        for (Mikan.Group group : groups) {
            Set<String> tags = new HashSet<>();
            List<List<Mikan.RegexItem>> regexList = new ArrayList<>();
            List<TorrentsInfo> items = group.getItems();
            for (TorrentsInfo item : items) {
                String name = item.getName();
                List<Mikan.RegexItem> regexItems = new ArrayList<>();
                for (String regex : regexItemList) {
                    if (!ReUtil.contains(regex, name)) {
                        continue;
                    }
                    String label = ReUtil.get(regex, name, 0);
                    label = label.toUpperCase();
                    Mikan.RegexItem regexItem = new Mikan.RegexItem(label, regex);
                    regexItems.add(regexItem);
                    tags.add(label);
                }
                regexItems = CollUtil.distinct(regexItems, GsonStatic::toJson, true);
                regexList.add(regexItems);
            }

            regexList = CollUtil.distinct(regexList, GsonStatic::toJson, true);
            group.setRegexList(regexList)
                    .setTags(tags);
        }
        return Result.success(groups);
    }

    @Auth
    @Operation(summary = "获取Mikan封面")
    @GetMapping("/mikanCover")
    public void MikanCover(@RequestParam("img") String img) {
        if (Base64.isBase64(img)) {
            img = img.replace(" ", "+");
            img = Base64.decodeStr(img);
        }
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
