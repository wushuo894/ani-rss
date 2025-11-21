package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.service.ClearService;
import ani.rss.util.basic.MyFileUtil;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 缓存清理
 */
@Slf4j
@Auth
@Path("/clearCache")
public class ClearCacheAction implements BaseAction {

    @Override
    public synchronized void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        File configDir = ConfigUtil.getConfigDir();
        String configDirStr = MyFileUtil.getAbsolutePath(configDir);

        Set<String> covers = AniUtil.ANI_LIST
                .stream()
                .map(Ani::getCover)
                .map(s -> MyFileUtil.getAbsolutePath(new File(configDirStr + "/files/" + s)))
                .collect(Collectors.toSet());

        FileUtil.mkdir(configDirStr + "/files");
        FileUtil.mkdir(configDirStr + "/img");

        Set<File> files = FileUtil.loopFiles(configDirStr + "/files")
                .stream()
                .filter(file -> {
                    String fileName = MyFileUtil.getAbsolutePath(file);
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
            ClearService.clearParentFile(file);
        }

        FileUtil.del(configDirStr + "/img");

        resultSuccessMsg("清理完成, 共清理{}MB", NumberUtil.decimalFormat("0.00", sumSize / 1024.0 / 1024.0));
    }

}
