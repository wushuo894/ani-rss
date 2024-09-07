package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;

import java.io.IOException;

@Auth(value = false)
@Path("/login")
public class LoginAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Login myLogin = getBody(Login.class);
        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();

        String myUsername = myLogin.getUsername();
        String myPassword = myLogin.getPassword();

        Assert.notBlank(myUsername, "登录密码不能为空");
        Assert.notBlank(myPassword, "用户名不能为空");

        String username = login.getUsername();
        String password = login.getPassword();

        if (username.equals(myUsername) && password.equals(myPassword)) {
            String s = MD5.create().digestHex(username + ":" + password);
            resultSuccess(s);
            return;
        }
        resultError();
    }
}
