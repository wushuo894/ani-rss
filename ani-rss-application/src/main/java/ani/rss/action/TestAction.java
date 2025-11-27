package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.web.auth.fun.IpWhitelist;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

/**
 * 用于检测是否处于白名单内
 */
@Auth(false)
@Path("/test")
public class TestAction implements BaseAction {
    private final IpWhitelist ipWhitelist = new IpWhitelist();

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Boolean b = ipWhitelist.apply(request);
        if (b) {
            resultSuccess();
            return;
        }
        resultError();
    }
}
