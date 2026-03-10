package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Result;
import ani.rss.task.FfmpegTask;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FfmpegController extends BaseController {

    @Auth
    @Operation(summary = "转码队列状态")
    @PostMapping("/ffmpegQueue")
    public Result<Map<String, Object>> ffmpegQueue() {
        // 等待队列（尚未开始处理）
        List<String> queue = new ArrayList<>();
        FfmpegTask.QUEUE.forEach(t -> queue.add(t.getName()));

        // 当前并行处理中的任务列表
        List<Map<String, Object>> active = new ArrayList<>();
        FfmpegTask.ACTIVE_TASKS.forEach((torrent, task) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("torrent", torrent);
            item.put("file", task.getFile());
            item.put("progress", task.getProgress());
            active.add(item);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", active);
        result.put("queue", queue);
        return Result.success(result);
    }

}
