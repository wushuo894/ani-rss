package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
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

        Assert.notBlank(myUsername, "密码不能为空");
        Assert.notBlank(myPassword, "用户名不能为空");

        String username = login.getUsername();
        String password = login.getPassword();

        String ip = getIp(request);

        if (username.equals(myUsername) && password.equals(myPassword)) {
            log.info("登录成功 {} ip: {}", username, ip);
            String s = MD5.create().digestHex(username + ":" + password);
            resultSuccess(s);
            return;
        }
        log.warn("登陆失败 {} ip: {}", myUsername, ip);
        ThreadUtil.sleep(RandomUtil.randomInt(500, 5000));
        resultErrorMsg("用户名或密码错误");
    }

    public String getIp(HttpServerRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StrUtil.isBlank(ip)) {
                continue;
            }
            return ip;
        }
        return "未知";
    }

}
