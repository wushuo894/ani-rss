package ani.rss.action;

import ani.rss.util.other.ConfigUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 自定义js
 */
@Slf4j
@Auth(value = false)
@Path("/custom.js")
public class CustomJsAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        response.setHeader(Header.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(Header.PRAGMA, "no-cache");
        response.setHeader("Expires", "0");

        String customJs = ConfigUtil.CONFIG.getCustomJs();
        customJs = StrUtil.blankToDefault(customJs, "// empty js");
        String contentType = "application/javascript; charset=utf-8";
        response.write(customJs, contentType);
    }
}
