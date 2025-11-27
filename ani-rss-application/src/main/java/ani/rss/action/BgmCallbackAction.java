package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

@Auth
@Path("/bgm/oauth/callback")
public class BgmCallbackAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String code = request.getParam("code");
        Config config = ConfigUtil.CONFIG;
        String bgmAppID = config.getBgmAppID();
        String bgmAppSecret = config.getBgmAppSecret();
        String bgmRedirectUri = config.getBgmRedirectUri();

        Map<String, String> map = Map.of(
                "grant_type", "authorization_code",
                "client_id", bgmAppID,
                "client_secret", bgmAppSecret,
                "code", code,
                "redirect_uri", bgmRedirectUri
        );

        HttpReq.post("https://bgm.tv/oauth/access_token")
                .body(GsonStatic.toJson(map))
                .then(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    String accessToken = jsonObject.get("access_token").getAsString();
                    String refreshToken = jsonObject.get("refresh_token").getAsString();
                    config.setBgmToken(accessToken)
                            .setBgmRefreshToken(refreshToken);
                });
        ConfigUtil.sync();

        resultSuccessMsg("授权成功, 现在你可以关闭此窗口");
    }
}
