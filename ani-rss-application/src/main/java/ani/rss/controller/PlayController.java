package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.entity.PlayItem;
import ani.rss.entity.web.Result;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
public class PlayController extends BaseController {

    @Resource
    private DownloadService downloadService;

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

        String downloadPath = downloadService.getDownloadPath(ani);
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

        String videoFileName = file.getName();
        long lastModified = file.lastModified();
        String formatSize = FileUtils.formatSize(file);
        String absolutePath = FileUtils.getAbsolutePath(file);

        PlayItem playItem = new PlayItem();
        playItems.add(playItem);
        playItem.setFilename(absolutePath)
                .setName(videoFileName)
                .setTitle(videoFileName)
                .setLastModify(lastModified)
                .setEpisode(1.0)
                .setFormatSize(formatSize)
                .setExtName(extName);

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

}
