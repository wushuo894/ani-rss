package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.config.CronConfig;
import ani.rss.entity.Config;
import ani.rss.entity.Global;
import ani.rss.entity.ProxyTest;
import ani.rss.entity.web.ContentType;
import ani.rss.entity.web.Result;
import ani.rss.service.ConfigService;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class ConfigController extends BaseController {

    @Resource
    private CronConfig cronConfig;

    @Resource
    private ConfigService configService;

    @Auth
    @Operation(summary = "获取设置")
    @PostMapping("/config")
    public Result<Config> config() {
        Config config = configService.config();
        return Result.success(config);
    }

    @Auth
    @Operation(summary = "修改设置")
    @PostMapping("/setConfig")
    public Result<Void> setConfig(@RequestBody Config newConfig) {
        configService.setConfig(newConfig);
        return Result.success("修改成功");
    }

    @Auth
    @Operation(summary = "清理缓存")
    @PostMapping("/clearCache")
    public Result<Void> clearCache() {
        String formatSize = configService.clearCache();
        return Result.success("清理完成, 共清理 {}", formatSize);
    }

    @Auth
    @Operation(summary = "更新trackers")
    @PostMapping("/trackersUpdate")
    public Result<Void> trackersUpdate(@RequestBody Config config) {
        cronConfig.updateTrackers(config);
        return Result.success();
    }

    @Auth
    @Operation(summary = "代理测试")
    @PostMapping("/testProxy")
    public Result<ProxyTest> testProxy(@RequestParam("url") String url, @RequestBody Config config) {
        return Result.success(configService.testProxy(url, config));
    }

    @Auth
    @Operation(summary = "下载器测试")
    @PostMapping("/downloadLoginTest")
    public Result<Void> downloadLoginTest(@RequestBody Config config) {
        Boolean login = configService.downloadLoginTest(config);
        if (login) {
            return Result.success("登录成功");
        }
        return Result.error("登录失败");
    }

    @Operation(summary = "存活测试")
    @RequestMapping("/ping")
    public Result<Void> ping() {
        return Result.success();
    }

    @Operation(summary = "自定义JS")
    @GetMapping("/custom.js")
    public void customJs() {
        HttpServletResponse response = Global.RESPONSE.get();
        setCacheControl(response, 0);

        String customJs = ConfigUtil.CONFIG.getCustomJs();
        customJs = StrUtil.blankToDefault(customJs, "// empty js");

        write(200, ContentType.JAVASCRIPT, customJs);
    }

    @Operation(summary = "自定义CSS")
    @GetMapping("/custom.css")
    public void customCss() {
        HttpServletResponse response = Global.RESPONSE.get();
        setCacheControl(response, 0);

        String customCss = ConfigUtil.CONFIG.getCustomCss();
        customCss = StrUtil.blankToDefault(customCss, "/* empty css */");

        write(200, ContentType.TEXT_CSS, customCss);
    }
}
