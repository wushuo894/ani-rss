package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 自定义css
 */
@Slf4j
@Auth(value = false)
@Path("/custom.css")
public class CustomCssAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        response.setContentType("text/css");
        response.setHeader(Header.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(Header.PRAGMA, "no-cache");
        response.setHeader("Expires", "0");
        @Cleanup
        OutputStream out = response.getOut();
        IoUtil.writeUtf8(out, true, ConfigUtil.CONFIG.getCustomCss());
    }
}
