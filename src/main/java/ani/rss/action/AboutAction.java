package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.util.UpdateUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

@Auth
@Slf4j
@Path("/about")
public class AboutAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        resultSuccess(UpdateUtil.about());
    }


}
