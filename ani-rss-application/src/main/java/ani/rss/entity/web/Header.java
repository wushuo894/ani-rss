package ani.rss.entity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "Http 头域")
public class Header implements Serializable {
    @Schema(description = "指定请求和响应遵循的缓存机制")
    public static String CACHE_CONTROL = "Cache-Control";

    @Schema(description = "用来包含实现特定的指令，最常用的是Pragma:no-cache。在HTTP/1.1协议中，它的含义和Cache- Control:no-cache相同")
    public static String PRAGMA = "Pragma";

    @Schema(description = "响应标头包含响应应被视为过期的日期/时间")
    public static String EXPIRES = "Expires";

    @Schema(description = "Content-Disposition")
    public static String CONTENT_DISPOSITION = "Content-Disposition";

    @Schema(description = "Authorization")
    public static String AUTHORIZATION = "Authorization";

    @Schema(description = "User-Agent")
    public static String USER_AGENT = "User-Agent";

    @Schema(description = "Content-Type")
    public static String CONTENT_TYPE = "Content-Type";

    @Schema(description = "Content-Range")
    public static String CONTENT_RANGE = "Content-Range";

    @Schema(description = "Accept-Ranges")
    public static String ACCEPT_RANGES = "Accept-Ranges";
}
