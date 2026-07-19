package ani.rss.service;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClearService {

    /**
     * 排除文件
     */
    private final List<String> excludeFileNames = List.of(
            ".DS_Store",
            "Thumbs.db"
    );

    /**
     * 清理文件夹
     *
     * @param dir 文件夹
     */
    public void clearDir(String dir) {
        clearDir(new File(dir));
    }

    /**
     * 清理文件夹
     *
     * @param dir 文件夹
     */
    public void clearDir(File dir) {
        clearDir(dir, true, true, 2);
    }

    /**
     * 清理文件夹
     *
     * @param dir   文件夹
     * @param image 排除图片
     * @param nfo   排除nfo
     * @param max   向上删除深度
     */
    public void clearDir(File dir, boolean image, boolean nfo, int max) {
        File parentFile = dir;
        for (int i = 0; i < max; i++) {
            if (Objects.isNull(parentFile)) {
                return;
            }
            if (!isEmpty(parentFile, image, nfo)) {
                // 不为空则不进行清理
                return;
            }
            log.info("清理空文件夹 {}", parentFile);
            FileUtil.del(parentFile);
            parentFile = parentFile.getParentFile();
        }
    }

    /**
     * 文件夹是否为空
     *
     * @param image 排除图片
     * @param nfo   排除nfo
     * @param dir   文件夹
     * @return 是否为空
     */
    public Boolean isEmpty(File dir, boolean image, boolean nfo) {
        List<File> list = FileUtils.listFileList(dir);

        long count = list.stream()
                .filter(f -> {
                    if (f.isDirectory()) {
                        return true;
                    }

                    if (image && FileUtils.isImageFormat(f.getName())) {
                        return false;
                    }

                    String extName = FileUtil.extName(f);
                    if (StrUtil.isBlank(extName)) {
                        return true;
                    }
                    if (nfo && extName.equalsIgnoreCase("nfo")) {
                        return false;
                    }
                    return true;
                })
                .filter(f -> {
                    String name = f.getName();
                    for (String excludeFileName : excludeFileNames) {
                        if (excludeFileName.equalsIgnoreCase(name)) {
                            return false;
                        }
                    }
                    return true;
                })
                .count();

        return count < 1;
    }

    /**
     * 清理残余封面
     *
     * @return 清理大小 bytes
     */
    public Long clearCover() {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = FileUtils.getAbsolutePath(configDir);
        File filesDir = new File(configDirStr, "files");
        File imgDir = new File(configDirStr, "img");

        FileUtil.mkdir(filesDir);
        FileUtil.mkdir(imgDir);

        Set<String> covers = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getCover)
                .map(s -> FileUtils.getAbsolutePath(Path.of(configDirStr, "files", s).toFile()))
                .collect(Collectors.toSet());

        Set<File> files = FileUtil.loopFiles(filesDir)
                .stream()
                .filter(file -> {
                    String fileName = FileUtils.getAbsolutePath(file);
                    return !covers.contains(fileName);
                }).collect(Collectors.toSet());
        long filesSize = files.stream()
                .mapToLong(File::length)
                .sum();
        long imgSize = FileUtil.size(imgDir);

        for (File file : files) {
            FileUtil.del(file);
            clearDir(file, false, true, 2);
        }

        return filesSize + imgSize;
    }

}
