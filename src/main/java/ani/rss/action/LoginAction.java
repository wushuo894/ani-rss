package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.Result;
import ani.rss.exception.ResultException;
import ani.rss.util.basic.MyCacheUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录
 */
@Slf4j
@Auth(value = false)
@Path("/login")
public class LoginAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        limitLoginAttempts(false);

        Login myLogin = getBody(Login.class);
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
            resultSuccess(s);
            return;
        }
        limitLoginAttempts(true);
        log.warn("登陆失败 {} ip: {}", myUsername, ip);
        ThreadUtil.sleep(RandomUtil.randomInt(500, 5000));
        resultErrorMsg("用户名或密码错误");
    }

    /**
     * 清除限制尝试次数
     */
    public static void clearLimitLoginAttempts() {
        String ip = AuthUtil.getIp();
        String key = "LimitLoginAttempts#" + ip;
        if (MyCacheUtil.containsKey(key)) {
            MyCacheUtil.remove(key);
        }
    }

    /**
     * 限制尝试次数
     *
     * @param isAdd 累加计数
     */
    public static void limitLoginAttempts(Boolean isAdd) {
        Config config = ConfigUtil.CONFIG;
        boolean limitLoginAttempts = config.getLimitLoginAttempts();
        if (!limitLoginAttempts) {
            return;
        }
        String ip = AuthUtil.getIp();
        String key = "LimitLoginAttempts#" + ip;

        // 1 天内将不再允许尝试
        long timeout = TimeUnit.DAYS.toMillis(1);

        if (!MyCacheUtil.containsKey(key)) {
            if (isAdd) {
                MyCacheUtil.put(key, new AtomicInteger(1), timeout);
            }
            return;
        }

        AtomicInteger countAtomicInteger = MyCacheUtil.get(key);
        int count = countAtomicInteger.getAndAdd(isAdd ? 1 : 0);

        // 失败时 时间将重新计时
        MyCacheUtil.put(key, countAtomicInteger, timeout);

        // 失败 10 次
        if (count < 10) {
            return;
        }

        log.debug("失败次数过多, 已限制登录 {}", ip);
        Result<Void> result = new Result<Void>()
                .setMessage(StrFormatter.format("失败次数过多, 已限制登录 {}", ip))
                .setCode(300);
        throw new ResultException(result);
    }

}
