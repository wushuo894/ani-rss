package ani.rss.service;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClearService {
    /**
     * 清理父级空文件夹
     *
     * @param path
     */
    public void clearParentFile(String path) {
        clearParentFile(new File(path));
    }

    /**
     * 清理父级空文件夹
     *
     * @param file
     */
    public void clearParentFile(File file) {
        if (file.exists()) {
            return;
        }
        File parentFile = file.getParentFile();
        List<String> list = Arrays.asList(ObjectUtil.defaultIfNull(parentFile.list(), new String[]{}));
        list = list.stream()
                .filter(f -> !f.endsWith(".nfo"))
                .filter(f -> !f.endsWith("-thumb.jpg"))
                .filter(f -> !f.equals("poster.jpg"))
                .filter(f -> !f.equals("clearlogo.png"))
                .filter(f -> !f.equals(".DS_Store"))
                .filter(f -> !f.equals("banner.jpg"))
                .filter(f -> !f.equals("season-specials-poster.jpg"))
                .filter(f -> !ReUtil.contains("^season\\d+-poster.jpg$", f))
                .filter(f -> !ReUtil.contains("^fanart\\d*.jpg$", f))
                .toList();
        if (!list.isEmpty()) {
            // 不为空则不进行清理
            return;
        }
        log.info("清理空文件夹 {}", parentFile);
        FileUtil.del(parentFile);
        clearParentFile(parentFile);
    }

    public Long clearCover() {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = FileUtils.getAbsolutePath(configDir);

        FileUtil.mkdir(configDirStr + "/files");
        FileUtil.mkdir(configDirStr + "/img");

        Set<String> covers = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getCover)
                .map(s -> FileUtils.getAbsolutePath(new File(configDirStr + "/files/" + s)))
                .collect(Collectors.toSet());

        Set<File> files = FileUtil.loopFiles(configDirStr + "/files")
                .stream()
                .filter(file -> {
                    String fileName = FileUtils.getAbsolutePath(file);
                    return !covers.contains(fileName);
                }).collect(Collectors.toSet());
        long filesSize = files.stream()
                .mapToLong(File::length)
                .sum();
        long imgSize = FileUtil.size(new File(configDirStr + "/img"));

        for (File file : files) {
            FileUtil.del(file);
            clearParentFile(file);
        }

        return filesSize + imgSize;
    }

}
