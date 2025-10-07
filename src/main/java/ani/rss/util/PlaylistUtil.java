package ani.rss.util;

import ani.rss.download.BaseDownload;
import ani.rss.entity.PlayItem;
import ani.rss.enums.StringEnum;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 播放列表工具
 */
public class PlaylistUtil {

    public static List<PlayItem> getPlayItem(File file) {
        List<PlayItem> playItems = new ArrayList<>();
        if (file.isDirectory()) {
            for (File listFile : ObjectUtil.defaultIfNull(file.listFiles(), new File[0])) {
                playItems.addAll(getPlayItem(listFile));
            }
            return playItems;
        }
        String extName = FileUtil.extName(file);
        if (StrUtil.isBlank(extName)) {
            return playItems;
        }
        if (!BaseDownload.videoFormat.contains(extName)) {
            return playItems;
        }
        if (!ReUtil.contains(StringEnum.SEASON_REG, file.getName())) {
            return playItems;
        }
        File[] files = ObjectUtil.defaultIfNull(file.getParentFile().listFiles(), new File[]{});
        List<PlayItem.Subtitles> subtitles = Arrays.stream(files)
                .filter(f -> List.of("ass", "srt").contains(ObjectUtil.defaultIfNull(FileUtil.extName(f), "")))
                .filter(f -> f.getName().startsWith(FileUtil.mainName(file.getName())))
                .map(f -> {
                    String name = StrUtil.blankToDefault(FileUtil.extName(FileUtil.mainName(f.getName())), "ass");
                    return new PlayItem.Subtitles()
                            .setName(name)
                            .setHtml(name.toUpperCase())
                            .setUrl(Base64.encode(FilePathUtil.getAbsolutePath(f)))
                            .setType(FileUtil.extName(f));
                }).toList();
        subtitles = CollUtil.distinct(subtitles, PlayItem.Subtitles::getName, true);

        PlayItem playItem = new PlayItem();
        playItem.setSubtitles(subtitles);
        playItem.setFilename(Base64.encode(FilePathUtil.getAbsolutePath(file)))
                .setName(file.getName())
                .setLastModify(file.lastModified())
                .setTitle(ReUtil.get(StringEnum.SEASON_REG, file.getName(), 0));
        Matcher matcher = PatternPool.get("E(\\d+)").matcher(file.getName());
        if (matcher.find()) {
            playItem.setEpisodeNumber(Integer.valueOf(matcher.group(1)));
        }
        playItems.add(playItem);
        return playItems;
    }
}
