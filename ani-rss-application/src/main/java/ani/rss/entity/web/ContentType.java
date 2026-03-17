package ani.rss.entity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "ContentType")
public class ContentType implements Serializable {
    @Schema(description = "标准表单编码 application/x-www-form-urlencoded")
    public static String FORM_URLENCODED = "application/x-www-form-urlencoded";

    @Schema(description = "文件上传编码 multipart/form-data")
    public static String MULTIPART = "multipart/form-data";

    @Schema(description = "application/json")
    public static String JSON = "application/json";

    @Schema(description = "application/xml")
    public static String XML = "application/xml";

    @Schema(description = "text/plain")
    public static String TEXT_PLAIN = "text/plain";

    @Schema(description = "text/xml")
    public static String TEXT_XML = "text/xml";

    @Schema(description = "text/html")
    public static String TEXT_HTML = "text/html; charset=UTF-8";

    @Schema(description = "application/octet-stream")
    public static String OCTET_STREAM = "application/octet-stream";

    @Schema(description = "text/event-stream")
    public static String EVENT_STREAM = "text/event-stream";

    @Schema(description = "text/css")
    public static String TEXT_CSS = "text/css";

    @Schema(description = "application/javascript")
    public static String JAVASCRIPT = "application/javascript; charset=utf-8";

    @Schema(description = "video/x-matroska")
    public static String VIDEO_X_MATROSKA = "video/x-matroska";
}
