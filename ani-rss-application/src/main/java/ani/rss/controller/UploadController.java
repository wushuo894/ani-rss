package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.Global;
import ani.rss.entity.Result;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@RestController
public class UploadController extends BaseController {
    @Auth
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        HttpServletRequest request = Global.REQUEST.get();
        String type = request.getParameter("type");
        byte[] fileContent = file.getBytes();
        if ("getBase64".equals(type)) {
            return Result.success(r ->
                    r.setData(Base64.encode(fileContent))
            );
        }

        String s = SecureUtil.md5(new ByteArrayInputStream(fileContent));
        String fileName = file.getOriginalFilename();
        String saveName = s + "." + FileUtil.extName(fileName);

        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/" + s.charAt(0));
        FileUtil.writeBytes(fileContent, configDir + "/files/" + s.charAt(0) + "/" + saveName);
        return new Result<>()
                .setMessage("上传完成")
                .setData(s.charAt(0) + "/" + saveName);
    }
}
