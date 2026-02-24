package ani.rss.auth.fun;

import ani.rss.entity.Login;
import ani.rss.util.other.AuthUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * 请求头鉴权
 */
public class Header implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        String s = request.getHeader("Authorization");
        if (StrUtil.isBlank(s)) {
            return false;
        }
        Login login = AuthUtil.getLogin();
        String auth = AuthUtil.getAuth(login);
        if (StrUtil.equals(auth, s)) {
            // 刷新有效时间
            AuthUtil.resetTime();
            return true;
        }
        return false;
    }
}
