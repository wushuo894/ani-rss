package ani.rss.auth.fun;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.auth.util.AuthUtil;
import ani.rss.entity.Login;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;

import java.util.function.Function;

@Auth(type = AuthType.FORM)
public class Form implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
        String s = request.getParam("s");
        Login login = AuthUtil.getLogin();
        String auth = AuthUtil.getAuth(login);
        return StrUtil.equals(auth, s);
    }
}
