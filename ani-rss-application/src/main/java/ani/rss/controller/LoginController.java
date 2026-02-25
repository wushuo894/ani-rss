package ani.rss.controller;

import ani.rss.commons.CacheUtils;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.Result;
import ani.rss.util.other.AuthUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController extends BaseController {

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody Login myLogin) {
        AuthUtil.limitLoginAttempts(false);

        Config config = ConfigUtil.CONFIG;
        Login login = config.getLogin();

        String myUsername = myLogin.getUsername();
        String myPassword = myLogin.getPassword();

        Assert.notBlank(myUsername, "用户名不能为空");
        Assert.notBlank(myPassword, "密码不能为空");

        String username = login.getUsername();
        String password = login.getPassword();

        // 一个令牌只能用于一个ip
        String ip = AuthUtil.getIp();
        if (config.getVerifyLoginIp()) {
            myLogin.setIp(ip);
        } else {
            myLogin.setIp("");
        }

        if (username.equals(myUsername) && password.equals(myPassword)) {
            AuthUtil.resetKey();
            clearLimitLoginAttempts();
            log.info("登录成功 {} ip: {}", username, ip);
            String s = AuthUtil.getAuth(myLogin);
            return new Result<String>()
                    .setCode(200)
                    .setMessage("登录成功")
                    .setData(s);
        }
        AuthUtil.limitLoginAttempts(true);
        log.warn("登陆失败 {} ip: {}", myUsername, ip);
        ThreadUtil.sleep(RandomUtil.randomInt(500, 5000));
        return Result.error("用户名或密码错误");
    }

    /**
     * 清除限制尝试次数
     */
    private void clearLimitLoginAttempts() {
        String ip = AuthUtil.getIp();
        String key = "LimitLoginAttempts#" + ip;
        if (CacheUtils.containsKey(key)) {
            CacheUtils.remove(key);
        }
    }
}
