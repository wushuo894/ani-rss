package ani.rss.auth.fun;

import ani.rss.auth.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Function;

/**
 * 表单鉴权
 */
public class Form implements Function<HttpServletRequest, Boolean> {
    @Override
    public Boolean apply(HttpServletRequest request) {
        String s = request.getParameter("s");
        return AuthUtil.verify(s);
    }
}
