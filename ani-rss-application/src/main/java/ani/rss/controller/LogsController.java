package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Global;
import ani.rss.entity.Log;
import ani.rss.entity.Result;
import ani.rss.util.basic.LogUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.Header;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
public class LogsController extends BaseController {
    List<Log> LOG_LIST = LogUtil.LOG_LIST;

    @Auth
    @Operation(summary = "日志")
    @PostMapping("/logs")
    public Result<List<Log>> list() {
        return Result.success(LOG_LIST);
    }

    @Auth
    @Operation(summary = "清理日志")
    @PostMapping("/clearLogs")
    public Result<Void> clearLogs() {
        LOG_LIST.clear();
        log.info("清理日志");
        return Result.success();
    }

    @Auth
    @Operation(summary = "下载日志")
    @GetMapping("/downloadLogs")
    public void downloadLogs() throws IOException {
        File configDir = ConfigUtil.getConfigDir();
        String logsPath = configDir + "/logs";

        String filename = "logs.zip";

        String contentType = getContentType(filename);

        HttpServletResponse response = Global.RESPONSE.get();

        response.setContentType(contentType);
        response.setHeader(Header.CONTENT_DISPOSITION.toString(), StrFormatter.format("inline; filename=\"{}\"", filename));

        @Cleanup
        OutputStream outputStream = response.getOutputStream();

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
