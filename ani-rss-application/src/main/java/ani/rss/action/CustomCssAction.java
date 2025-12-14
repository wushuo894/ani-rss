package ani.rss.action;

import ani.rss.util.other.ConfigUtil;
import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import cn.hutool.http.Header;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 自定义css
 */
@Slf4j
@Auth(value = false)
@Path("/custom.css")
public class CustomCssAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        response.setHeader(Header.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(Header.PRAGMA, "no-cache");
        response.setHeader("Expires", "0");

        String customCss = ConfigUtil.CONFIG.getCustomCss();
        String contentType = "text/css";
        response.write(customCss, contentType);
    }
}
