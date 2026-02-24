package ani.rss.auth.fun;

import ani.rss.entity.Login;
import ani.rss.util.other.AuthUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * 表单鉴权
 */
public class Form implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        String s = request.getParameter("s");
        Login login = AuthUtil.getLogin();
        String auth = AuthUtil.getAuth(login);
        return StrUtil.equals(auth, s);
    }
}
