package ani.rss.auth.fun;

import ani.rss.action.BaseAction;
import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Login;
import ani.rss.entity.Result;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;

import java.util.function.Function;

@Auth(type = AuthType.HEADER)
public class Header implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
        String s = request.getHeader("Authorization");
        if (StrUtil.isBlank(s)) {
            BaseAction.staticResult(new Result<>().setCode(403).setMessage("未登录"));
            return false;
        }
        Login login = AuthUtil.getLogin();
        String auth = AuthUtil.getAuth(login);
        if (StrUtil.equals(auth, s)) {
            return true;
        }
        BaseAction.staticResult(new Result<>().setCode(403).setMessage("登录失效"));
        return false;
    }
}
