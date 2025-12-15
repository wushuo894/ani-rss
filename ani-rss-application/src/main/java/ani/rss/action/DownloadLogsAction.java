package ani.rss.action;

import ani.rss.util.other.ConfigUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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

        String filename = "logs.zip";

        String contentType = getContentType(filename);

        response.setContentType(contentType);
        response.setHeader(Header.CONTENT_DISPOSITION, StrFormatter.format("inline; filename=\"{}\"", filename));

        @Cleanup
        OutputStream outputStream = response.getOut();

        ZipUtil.zip(outputStream, StandardCharsets.UTF_8, false, name -> {
            if (FileUtil.isDirectory(name)) {
                return true;
            }
            String extName = FileUtil.extName(name);
            if (StrUtil.isBlank(extName)) {
                return false;
            }
            return extName.equals("log");
        }, new File(logsPath));
    }
}
