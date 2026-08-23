package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.web.Result;
import ani.rss.entity.web.ResultCode;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.SecureUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Cleanup;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class UploadController extends BaseController {
    @Auth
    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        @Cleanup
        InputStream inputStream = file.getInputStream();

        File tempFile = FileUtil.createTempFile();
        FileUtil.writeFromStream(inputStream, tempFile);

        String s = SecureUtil.md5(tempFile);
        String fileName = file.getOriginalFilename();
        String saveName = s + "." + FileUtil.extName(fileName);

        File configDir = ConfigUtil.getConfigDir();
        File dir = new File(configDir, "/files/" + s.charAt(0));
        File saveFile = new File(dir, saveName);
        FileUtil.move(tempFile, saveFile, true);
        return new Result<>()
                .setCode(ResultCode.HTTP_OK)
                .setMessage("上传完成")
                .setData(s.charAt(0) + "/" + saveName);
    }

    @Auth
    @Operation(summary = "上传并读取为 base64")
    @PostMapping(value = "/uploadAndReadToBase64", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAndReadToBase64(@RequestParam("file") MultipartFile file) throws IOException {
        @Cleanup
        InputStream inputStream = file.getInputStream();

        String base64 = Base64.encode(inputStream);

        return new Result<String>().setData(base64);
    }

    @Auth
    @Operation(summary = "上传并读取")
    @PostMapping(value = "/uploadAndRead", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAndRead(@RequestParam("file") MultipartFile file) throws IOException {
        @Cleanup
        InputStream inputStream = file.getInputStream();

        String string = IoUtil.readUtf8(inputStream);

        return new Result<String>().setData(string);
    }
}
