package ani.rss.action;


import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Result;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.net.multipart.UploadFile;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * 上传文件
 */
@Slf4j
@Auth
@Path("/upload")
public class UploadAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String type = request.getParam("type");
        MultipartFormData multipart = request.getMultipart();
        UploadFile file = multipart.getFile("file");
        if (file.size() > 1024 * 1024 * 50) {
            resultErrorMsg("文件大小超过 50M");
            return;
        }
        byte[] fileContent = file.getFileContent();
        if ("getBase64".equals(type)) {
            resultSuccess(Base64.encode(fileContent));
            return;
        }

        String s = SecureUtil.md5(new ByteArrayInputStream(fileContent));
        String fileName = file.getFileName();
        String saveName = s + "." + FileUtil.extName(fileName);

        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/" + s.charAt(0));
        FileUtil.writeBytes(fileContent, configDir + "/files/" + s.charAt(0) + "/" + saveName);
        resultSuccess(new Result<>().setMessage("上传完成").setData(s.charAt(0) + "/" + saveName));
    }
}
