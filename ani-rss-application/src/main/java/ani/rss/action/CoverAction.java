package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 刷新封面
 */
@Slf4j
@Auth
@Path("/cover")
public class CoverAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Ani ani = getBody(Ani.class);
        String s = AniUtil.saveJpg(ani.getImage(), true);
        resultSuccess(s);
    }
}
