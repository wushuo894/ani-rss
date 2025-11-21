package ani.rss.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ClearService {
    /**
     * 清理父级空文件夹
     *
     * @param path
     */
    public static void clearParentFile(String path) {
        clearParentFile(new File(path));
    }

    /**
     * 清理父级空文件夹
     *
     * @param file
     */
    public static void clearParentFile(File file) {
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
}
