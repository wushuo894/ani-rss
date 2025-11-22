package ani.rss.auth.fun;

import ani.rss.entity.Login;
import ani.rss.util.AuthUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;

import java.util.function.Function;

/**
 * 表单鉴权
 */
public class Form implements Function<HttpServerRequest, Boolean> {
    @Override
    public Boolean apply(HttpServerRequest request) {
        String s = request.getParam("s");
        Login login = AuthUtil.getLogin();
        String auth = AuthUtil.getAuth(login);
        return StrUtil.equals(auth, s);
    }
}
