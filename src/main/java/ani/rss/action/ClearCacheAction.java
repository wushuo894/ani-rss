package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.basic.FilePathUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缓存清理
 */
@Slf4j
@Auth
@Path("/clearCache")
public class ClearCacheAction implements BaseAction {

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

    @Override
    public synchronized void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = FilePathUtil.getAbsolutePath(configDir);

        Set<String> covers = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getCover)
                .map(s -> FilePathUtil.getAbsolutePath(new File(configDirStr + "/files/" + s)))
                .collect(Collectors.toSet());

        FileUtil.mkdir(configDirStr + "/files");
        FileUtil.mkdir(configDirStr + "/img");

        Set<File> files = FileUtil.loopFiles(configDirStr + "/files")
                .stream()
                .filter(file -> {
                    String fileName = FilePathUtil.getAbsolutePath(file);
                    return !covers.contains(fileName);
                }).collect(Collectors.toSet());
        long filesSize = files.stream()
                .mapToLong(File::length)
                .sum();
        long imgSize = FileUtil.size(new File(configDirStr + "/img"));

        long sumSize = filesSize + imgSize;

        if (sumSize < 1) {
            resultSuccessMsg("清理完成, 共清理{}MB", 0);
            return;
        }

        for (File file : files) {
            FileUtil.del(file);
            clearParentFile(file);
        }

        FileUtil.del(configDirStr + "/img");

        resultSuccessMsg("清理完成, 共清理{}MB", NumberUtil.decimalFormat("0.00", sumSize / 1024.0 / 1024.0));
    }

}
