package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.commons.ExceptionUtil;
import ani.rss.entity.Ani;
import ani.rss.util.other.AniUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


/**
 * 根据rss解析为订阅
 */
@Slf4j
@Auth
@Path("/rss")
public class RssAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        if (!req.getMethod().equals("POST")) {
            return;
        }
        Ani ani = getBody(Ani.class);
        String url = ani.getUrl();
        String type = ani.getType();
        String bgmUrl = ani.getBgmUrl();
        Assert.notBlank(url, "RSS地址 不能为空");
        if (!ReUtil.contains("http(s*)://", url)) {
            url = "https://" + url;
        }
        url = URLUtil.decode(url, "utf-8");
        try {
            Ani newAni = AniUtil.getAni(url, type, bgmUrl);
            resultSuccess(newAni);
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            resultErrorMsg("RSS解析失败 {}", message);
        }
    }
}
