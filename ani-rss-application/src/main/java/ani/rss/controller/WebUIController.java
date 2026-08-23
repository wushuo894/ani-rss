package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.UpdateInfo;
import ani.rss.entity.web.Result;
import ani.rss.service.WebUIService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebUIController extends BaseController {

    private final WebUIService webUIService;

    @Auth
    @Operation(summary = "上传 WebUI")
    @PostMapping(value = "/webui/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> upload(@RequestParam("file") MultipartFile file) {
        webUIService.upload(file);
        return Result.success("WebUI 上传完成, 请刷新页面");
    }

    @Auth
    @Operation(summary = "删除 WebUI")
    @PostMapping("/webui/delete")
    public Result<Void> delete() {
        webUIService.delete();
        return Result.success("WebUI 删除完成");
    }

    @Auth
    @Operation(summary = "获取 WebUI 更新")
    @PostMapping("/webui/getUpdate")
    public Result<UpdateInfo> getUpdate() {
        UpdateInfo updateInfo = webUIService.getUpdate();
        Objects.requireNonNull(updateInfo, "无 WebUI 更新");
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
