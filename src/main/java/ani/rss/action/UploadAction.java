package ani.rss.action;


import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Result;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.multipart.MultipartFormData;
import cn.hutool.core.net.multipart.UploadFile;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
@Auth(type = AuthType.FORM)
@Path("/upload")
public class UploadAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        MultipartFormData multipart = request.getMultipart();
        UploadFile file = multipart.getFile("file");
        if (file.size() > 1024 * 1024) {
            resultErrorMsg("文件大小超过 1M");
            return;
        }
        byte[] fileContent = file.getFileContent();
        String s = MD5.create().digestHex(fileContent);
        String fileName = file.getFileName();
        String saveName = s + "." + FileUtil.extName(fileName);

        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/" + s.charAt(0));
        FileUtil.writeBytes(fileContent, configDir + "/files/" + s.charAt(0) + "/" + saveName);
        resultSuccess(new Result<>().setMessage("上传完成").setData(s.charAt(0) + "/" + saveName));
    }
}
