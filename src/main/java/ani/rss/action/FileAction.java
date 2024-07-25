package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import lombok.Cleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Path("/file")
public class FileAction implements Action {
    private final Log log = Log.get(FileAction.class);

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        String filename = req.getParam("filename");
        if (StrUtil.isBlank(filename)) {
            res.sendOk();
            return;
        }
        String mimeType = FileUtil.getMimeType(filename);
        res.setContentType(mimeType);
        res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        File configDir = ConfigUtil.getConfigDir();
        @Cleanup
        FileInputStream inputStream = IoUtil.toStream(new File(configDir + "/files/" + filename));
        @Cleanup
        OutputStream out = res.getOut();
        IoUtil.copy(inputStream, out);
    }
}
