package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.UpdateInfo;
import ani.rss.entity.web.Result;
import ani.rss.service.WebUIService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebUIController extends BaseController {

    private final WebUIService webUIService;

    @Auth
    @Operation(summary = "获取 WebUI 更新")
    @PostMapping("/webui/getUpdate")
    public Result<UpdateInfo> getUpdate() {
        UpdateInfo updateInfo = webUIService.getUpdate();
        if (Objects.isNull(updateInfo)) {
            return Result.error("ERROR");
        }
        return Result.success(updateInfo);
    }

    @Auth
    @Operation(summary = "更新 WebUI")
    @PostMapping("/webui/update")
    public Result<Void> update() {
        webUIService.update();
        return Result.success("WebUI 更新完成, 请刷新页面");
    }

}
