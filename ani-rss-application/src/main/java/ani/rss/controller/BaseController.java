package ani.rss.controller;

import ani.rss.entity.Global;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;

import java.io.OutputStream;

public class BaseController {
    /**
     * 根据文件扩展名获得ContentType
     *
     * @param filename 文件名
     * @return ContentType
     */
    public static String getContentType(String filename) {
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

    public static void writeNotFound() {
        writeHtml(HttpStatus.HTTP_NOT_FOUND, "404 Not Found !");
    }

    public static void writeHtml(Integer status, String text) {
        HttpServletResponse response = Global.RESPONSE.get();
        String html = ResourceUtil.readUtf8Str("template.html");
        html = html.replace("${text}", text);
        try {
            response.setStatus(status);
            response.setContentType("text/html;charset=UTF-8");

            @Cleanup
            OutputStream outputStream = response.getOutputStream();
            IoUtil.writeUtf8(outputStream, true, html);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
