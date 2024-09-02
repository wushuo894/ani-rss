package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.util.MikanUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Path("/mikan/group")
public class GroupAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String url = request.getParam("url");
        resultSuccess(MikanUtil.getGroups(url));
    }
}
