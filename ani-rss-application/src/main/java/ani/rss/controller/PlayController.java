package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.entity.Result;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.matthewn4444.ebml.EBMLReader;
import com.matthewn4444.ebml.subtitles.Subtitles;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Cleanup;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
public class PlayController {

    @Auth
    @Operation(summary = "获取内封字幕")
    @PostMapping("/getSubtitles")
    public Result<List<PlayItem.Subtitles>> getSubtitles(@RequestBody Map<String, String> map) throws IOException {
        String file = map.get("file");
        Assert.notBlank(file);

        if (Base64.isBase64(file)) {
            file = Base64.decodeStr(file);
        }

        List<PlayItem.Subtitles> subtitlesList = new ArrayList<>();

        String extName = FileUtil.extName(file);
        if (StrUtil.isBlank(extName)) {
            return Result.success(subtitlesList);
        }

        if (!"mkv".equals(extName)) {
            return Result.success(subtitlesList);
        }

        Assert.isTrue(FileUtil.exist(file), "视频文件不存在");

        @Cleanup
        EBMLReader reader = new EBMLReader(file);
        if (!reader.readHeader()) {
            return Result.success(subtitlesList);
        }
        reader.readTracks();
        reader.readCues();

        for (int i = 0; i < reader.getCuesCount(); i++) {
            reader.readSubtitlesInCueFrame(i);
        }

        List<Subtitles> subtitles = reader.getSubtitles();
        for (Subtitles subtitle : subtitles) {
            String name = subtitle.getName();
            String presentableName = subtitle.getPresentableName();
            String contents = subtitle.getContentsToVTT();
            PlayItem.Subtitles sub = new PlayItem.Subtitles();
            sub.setContent(contents)
                    .setName(name)
                    .setHtml(presentableName)
                    .setUrl("")
                    .setType("vtt");
            subtitlesList.add(sub);
        }

        return Result.success(subtitlesList);
    }

    @Auth
    @Operation(summary = "获取视频列表")
    @PostMapping("/playList")
    public Result<List<PlayItem>> playList(@RequestBody Ani ani) {
        String url = ani.getUrl();
        Optional<Ani> first = AniUtil.ANI_LIST
                .stream()
                .filter(it -> url.equals(it.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            return Result.error();
        }
        ani = first.get();

        String downloadPath = DownloadService.getDownloadPath(ani);
        List<PlayItem> collect = getPlayItem(new File(downloadPath));

        // 按照集数排序
        CollUtil.sort(collect, Comparator.comparingDouble(PlayItem::getEpisode));

        return Result.success(collect);
    }

    /**
     * 获取目录下的视频列表
     *
     * @param file 目录
     * @return 视频列表
     */
    public List<PlayItem> getPlayItem(File file) {
        List<PlayItem> playItems = new ArrayList<>();

        if (!file.exists()) {
            // 文件或目录不存在
            return playItems;
        }

        if (file.isDirectory()) {
            // 进行递归
            File[] files = FileUtils.listFiles(file);
            for (File itFile : files) {
                playItems.addAll(getPlayItem(itFile));
            }
            return playItems;
        }

        // 视频文件大小
        long videoFileLength = file.length();

        if (videoFileLength < 1024 * 1024 * 20) {
            // 视频文件小于 20MB 跳过
            return playItems;
        }

        String extName = FileUtil.extName(file);
        if (StrUtil.isBlank(extName)) {
            // 扩展名为空直接跳过
            return playItems;
        }
        if (!FileUtils.isVideoFormat(extName)) {
            // 过滤出视频文件
            return playItems;
        }

        // 查找同层级的字幕文件
        List<PlayItem.Subtitles> subtitles = getSubtitlesByVideo(file);

        String videoFileName = file.getName();
        long lastModified = file.lastModified();
        String size = videoFileLength / (1024 * 1024) + "M";
        String absolutePath = FileUtils.getAbsolutePath(file);

        PlayItem playItem = new PlayItem();
        playItems.add(playItem);
        playItem.setFilename(Base64.encode(absolutePath))
                .setName(videoFileName)
                .setTitle(videoFileName)
                .setLastModify(lastModified)
                .setEpisode(1.0)
                .setSize(size)
                .setExtName(extName)
                .setSubtitles(subtitles);

        if (ReUtil.contains(StringEnum.SEASON_REG, file.getName())) {
            // 如匹配正则则用正则取 S01E01
            String title = ReUtil.get(StringEnum.SEASON_REG, videoFileName, 0);
            // 集数
            String episode = ReUtil.get(StringEnum.SEASON_REG, videoFileName, 2);
            playItem.setTitle(title)
                    .setEpisode(Double.parseDouble(episode));
        }

        // 去重复
        playItems = CollUtil.distinct(playItems, PlayItem::getTitle, false);

        // 按照集数排序
        return playItems;
    }

    /**
     * 根据视频文件找到同层级的字幕
     *
     * @param videoFile 视频文件
     * @return 字幕列表
     */
    public List<PlayItem.Subtitles> getSubtitlesByVideo(File videoFile) {
        // 查找同层级的字幕文件
        File[] files = FileUtils.listFiles(videoFile.getParentFile());
        List<PlayItem.Subtitles> subtitles = Arrays.stream(files)
                .filter(sub -> {
                    String ext = FileUtil.extName(sub);
                    if (StrUtil.isBlank(ext)) {
                        return false;
                    }
                    // 浏览器仅支持 ass、srt
                    if (!List.of("ass", "srt").contains(ext)) {
                        return false;
                    }
                    // 视频主文件名
                    String videoMainName = FileUtil.mainName(videoFile);
                    // 字幕文件主文件名
                    String subMainName = FileUtil.mainName(sub);

                    // 字幕文件需与视频文件匹配 如 S01E01.chs.ass S01E01.mkv
                    return subMainName.startsWith(videoMainName);
                })
                .map(sub -> {
                    // 主文件名
                    String subMainName = FileUtil.mainName(sub.getName());
                    String absolutePath = FileUtils.getAbsolutePath(sub);
                    return new PlayItem.Subtitles()
                            .setName(subMainName)
                            .setHtml(subMainName.toUpperCase())
                            .setUrl(Base64.encode(absolutePath))
                            .setType(FileUtil.extName(sub));
                }).toList();
        // 去重复
        return CollUtil.distinct(subtitles, PlayItem.Subtitles::getName, true);
    }

}
