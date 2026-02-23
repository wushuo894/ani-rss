package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.EmbyViews;
import ani.rss.entity.NotificationConfig;
import ani.rss.entity.Result;
import ani.rss.util.other.EmbyUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/emby")
public class EmbyController {
    @Auth
    @Operation(summary = "获取媒体库")
    @PostMapping("/getViews")
    public Result<List<EmbyViews>> getViews(@RequestBody NotificationConfig notificationConfig) {
        List<EmbyViews> views = EmbyUtil.getViews(notificationConfig);
        return Result.success(views);
    }

    @Auth
    @Operation(summary = "刷新媒体库")
    @PostMapping("/refresh")
    public Result<Void> refresh(@RequestBody NotificationConfig notificationConfig) {
        EmbyUtil.refresh(notificationConfig);
        return Result.success();
    }
}
