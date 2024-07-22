package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

/**
 * 测试登录是否有效
 */
@Auth
@Path("/test")
public class TestAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        resultSuccess();
    }
}
