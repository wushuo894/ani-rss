package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Auth
@Path("/test")
public class TestAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        resultSuccess();
    }
}
