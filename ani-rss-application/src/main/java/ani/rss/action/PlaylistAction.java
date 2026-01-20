package ani.rss.action;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.web.auth.enums.AuthType;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 视频列表
 */
@Auth(type = {
        AuthType.IP_WHITE_LIST,
        AuthType.HEADER,
        AuthType.FORM,
        AuthType.API_KEY
})
@Slf4j
@Path("/playlist")
public class PlaylistAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        String url = ani.getUrl();
        Optional<Ani> first = AniUtil.ANI_LIST
                .stream()
                .filter(it -> url.equals(it.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            resultError();
            return;
        }
        ani = first.get();

        String downloadPath = DownloadService.getDownloadPath(ani);
        List<PlayItem> collect = getPlayItem(new File(downloadPath));

        // 按照集数排序
        CollUtil.sort(collect, Comparator.comparingDouble(PlayItem::getEpisode));

        resultSuccess(collect);
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
