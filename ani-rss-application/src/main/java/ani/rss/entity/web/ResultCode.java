package ani.rss.entity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "响应状态码")
public class ResultCode implements Serializable {

    @Schema(description = "HTTP Status-Code 200: OK.")
    public static final int HTTP_OK = 200;

    @Schema(description = "HTTP Status-Code 403: Forbidden.")
    public static final int HTTP_FORBIDDEN = 403;

    @Schema(description = "HTTP Status-Code 404: Not Found.")
    public static final int HTTP_NOT_FOUND = 404;

    @Schema(description = "HTTP Status-Code 500: Internal Server Error.")
    public static final int HTTP_INTERNAL_ERROR = 500;
}
