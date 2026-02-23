package ani.rss.controller;

import ani.rss.entity.Log;
import ani.rss.entity.Result;
import ani.rss.util.basic.LogUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class LogsController {
    List<Log> LOG_LIST = LogUtil.LOG_LIST;

    @Operation(summary = "日志")
    @PostMapping("/logs")
    public Result<List<Log>> list() {
        return Result.success(LOG_LIST);
    }

    @Operation(summary = "清理日志")
    @PostMapping("/clearLogs")
    public Result<Void> clearLogs() {
        LOG_LIST.clear();
        log.info("清理日志");
        return Result.success();
    }
}
