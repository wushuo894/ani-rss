package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.MavenUtils;
import ani.rss.entity.Global;
import ani.rss.entity.web.Header;
import ani.rss.entity.web.Result;
import ani.rss.service.BackupService;
import ani.rss.service.TaskService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.FileUtil;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
public class BackupController extends BaseController {
    @Resource
    private BackupService backupService;
    @Resource
    private TaskService taskService;

    @Auth
    @Operation(summary = "导出备份")
    @GetMapping("/exportBackup")
    public void backupBackup() throws IOException {
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
    @Operation(summary = "导入备份")
    @PostMapping(value = "/importBackup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> importBackup(@RequestParam("file") MultipartFile file) throws IOException {
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
