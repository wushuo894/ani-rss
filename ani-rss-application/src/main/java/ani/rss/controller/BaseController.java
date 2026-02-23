package ani.rss.controller;

import ani.rss.entity.Global;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;

public class BaseController {
    /**
     * 根据文件扩展名获得ContentType
     *
     * @param filename 文件名
     * @return ContentType
     */
    public String getContentType(String filename) {
        if (StrUtil.isBlank(filename)) {
            return ContentType.OCTET_STREAM.getValue();
        }

        String extName = FileUtil.extName(filename);

        if (StrUtil.isBlank(extName)) {
            return ContentType.OCTET_STREAM.getValue();
        }

        if (extName.equalsIgnoreCase("mkv")) {
            return "video/x-matroska";
        }

        String mimeType = FileUtil.getMimeType(filename);
        if (StrUtil.isNotBlank(mimeType)) {
            return mimeType;
        }

        return ContentType.OCTET_STREAM.getValue();
    }

    public void writeNotFound() {
        HttpServletResponse response = Global.RESPONSE.get();
        String html = ResourceUtil.readUtf8Str("template.html");
        html = html.replace("${text}", "404 Not Found !");
        try {
            response.sendError(HttpStatus.HTTP_NOT_FOUND, html);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
