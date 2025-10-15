package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 下载日志
 */
@Auth
@Path("/downloadLogs")
public class DownloadLogsAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        File configDir = ConfigUtil.getConfigDir();
        String logsPath = configDir + "/logs";
        File tempFile = FileUtil.createTempFile();
        ZipUtil.zip(tempFile, StandardCharsets.UTF_8, false, new File(logsPath));
        response.write(tempFile, "logs.zip");
    }
}
