package ani.rss.entity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "ContentType")
public class ContentType implements Serializable {
    @Schema(description = "标准表单编码 application/x-www-form-urlencoded")
    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

    @Schema(description = "文件上传编码 multipart/form-data")
    public static final String MULTIPART = "multipart/form-data";

    @Schema(description = "application/json")
    public static final String JSON = "application/json";

    @Schema(description = "application/xml")
    public static final String XML = "application/xml";

    @Schema(description = "text/plain")
    public static final String TEXT_PLAIN = "text/plain";

    @Schema(description = "text/xml")
    public static final String TEXT_XML = "text/xml";

    @Schema(description = "text/html")
    public static final String TEXT_HTML = "text/html; charset=UTF-8";

    @Schema(description = "application/octet-stream")
    public static final String OCTET_STREAM = "application/octet-stream";

    @Schema(description = "text/event-stream")
    public static final String EVENT_STREAM = "text/event-stream";

    @Schema(description = "text/css")
    public static final String TEXT_CSS = "text/css";

    @Schema(description = "application/javascript")
    public static final String JAVASCRIPT = "application/javascript; charset=utf-8";

    @Schema(description = "video/x-matroska")
    public static final String VIDEO_X_MATROSKA = "video/x-matroska";

    @Schema(description = "text/calendar")
    public static final String TEXT_CALENDAR = "text/calendar; charset=UTF-8";
}
