package ani.rss.auth.fun;

import ani.rss.auth.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * 请求头鉴权
 */
public class Header implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        String s = request.getHeader("Authorization");
        return AuthUtil.verify(s);
    }
}
