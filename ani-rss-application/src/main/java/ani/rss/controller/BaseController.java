package ani.rss.controller;

import ani.rss.entity.Global;
import ani.rss.entity.web.ContentType;
import ani.rss.entity.web.Header;
import ani.rss.entity.web.ResultCode;
import ani.rss.util.other.TemplateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Cleanup;

import java.io.OutputStream;
import java.util.Map;

public class BaseController {

    /**
     * 根据文件扩展名获得ContentType
     *
     * @param filename 文件名
     * @return ContentType
     */
    public static String getContentType(String filename) {
        if (StrUtil.isBlank(filename)) {
            return ContentType.OCTET_STREAM;
        }

        String extName = FileUtil.extName(filename);

        if (StrUtil.isBlank(extName)) {
            return ContentType.OCTET_STREAM;
        }

        if (extName.equalsIgnoreCase("mkv")) {
            return ContentType.VIDEO_X_MATROSKA;
        }

        String mimeType = FileUtil.getMimeType(filename);
        if (StrUtil.isNotBlank(mimeType)) {
            return mimeType;
        }

        return ContentType.OCTET_STREAM;
    }

    public static void setCacheControl(HttpServletResponse response, long maxAge) {
        if (maxAge > 0) {
            response.setHeader(Header.CACHE_CONTROL, "private, max-age=" + maxAge);
            return;
        }

        response.setHeader(Header.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(Header.PRAGMA, "no-cache");
        response.setHeader(Header.EXPIRES, "0");
    }

    public static void writeNotFound() {
        writeHtml(ResultCode.HTTP_NOT_FOUND, "404 Not Found !");
    }

    public static void writeHtml(Integer status, String text) {
        HttpServletResponse response = Global.RESPONSE.get();
        try {
            Map<String, String> map = Map.of("text", text);
            String html = TemplateUtil.render("text.html", map);

            response.setStatus(status);
            response.setContentType(ContentType.TEXT_HTML);
            response.setContentLength(html.length());

            @Cleanup
            OutputStream outputStream = response.getOutputStream();
            IoUtil.writeUtf8(outputStream, true, html);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
