package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;

@Slf4j
@Auth(value = false)
@Path("/customCss.css")
public class CustomCssAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        response.setContentType("text/css");
        @Cleanup
        OutputStream out = response.getOut();
        IoUtil.writeUtf8(out, true, ConfigUtil.CONFIG.getCustomCss());
    }
}
