package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.MavenUtils;
import ani.rss.config.CronConfig;
import ani.rss.entity.Config;
import ani.rss.entity.Global;
import ani.rss.entity.ProxyTest;
import ani.rss.entity.web.ContentType;
import ani.rss.entity.web.Header;
import ani.rss.entity.web.Result;
import ani.rss.service.BackupService;
import ani.rss.service.ConfigService;
import ani.rss.service.TaskService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
public class ConfigController extends BaseController {

    @Resource
    private CronConfig cronConfig;

    @Resource
    private BackupService backupService;

    @Resource
    private TaskService taskService;

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
    public void customJs() throws IOException {
        HttpServletResponse response = Global.RESPONSE.get();
        setCacheControl(response, 0);

        String customJs = ConfigUtil.CONFIG.getCustomJs();
        customJs = StrUtil.blankToDefault(customJs, "// empty js");

        response.setContentType(ContentType.JAVASCRIPT);
        response.setContentLength(customJs.length());
        @Cleanup
        OutputStream outputStream = response.getOutputStream();
        IoUtil.writeUtf8(outputStream, true, customJs);
    }

    @Operation(summary = "自定义CSS")
    @GetMapping("/custom.css")
    public void customCss() throws IOException {
        HttpServletResponse response = Global.RESPONSE.get();
        setCacheControl(response, 0);

        String customCss = ConfigUtil.CONFIG.getCustomCss();
        customCss = StrUtil.blankToDefault(customCss, "/* empty css */");

        response.setContentType(ContentType.TEXT_CSS);
        response.setContentLength(customCss.length());
        @Cleanup
        OutputStream outputStream = response.getOutputStream();
        IoUtil.writeUtf8(outputStream, true, customCss);
    }

    @Auth
    @Operation(summary = "导出设置")
    @GetMapping("/exportConfig")
    public void backupConfig() throws IOException {
        String version = MavenUtils.getVersion();
        String filename = StrUtil.format("ani-rss.backup.{}.zip", version);

        String contentType = getContentType(filename);

        HttpServletResponse response = Global.RESPONSE.get();

        response.setContentType(contentType);
        response.setHeader(Header.CONTENT_DISPOSITION, StrFormatter.format("inline; filename=\"{}\"", filename));

        @Cleanup
        OutputStream outputStream = response.getOutputStream();

        backupService.backup(outputStream);
    }

    @Auth
    @Operation(summary = "导入设置")
    @PostMapping(value = "/importConfig", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> importConfig(@RequestParam("file") MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.isTrue("zip".equals(extName), "导入格式异常");

        File configDir = ConfigUtil.getConfigDir();

        // 删除旧的种子记录
        FileUtil.del(configDir + "/torrents");

        @Cleanup
        InputStream inputStream = file.getInputStream();

        ZipUtil.unzip(inputStream, configDir, StandardCharsets.UTF_8);

        // 重新加载设置
        ConfigUtil.load();
        AniUtil.load();
        taskService.restart();

        return Result.success("导入成功");
    }
}
