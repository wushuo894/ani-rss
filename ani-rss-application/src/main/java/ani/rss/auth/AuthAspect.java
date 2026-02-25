package ani.rss.auth;

import ani.rss.annotation.Auth;
import ani.rss.entity.Global;
import ani.rss.entity.Result;
import ani.rss.exception.ResultException;
import ani.rss.util.other.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthAspect {
    @Before("@annotation(auth)")
    public void before(JoinPoint joinPoint, Auth auth) throws Exception {
        HttpServletRequest request = Global.REQUEST.get();
        if (AuthUtil.test(request, auth)) {
            // 鉴权通过
            return;
        }

        throw new ResultException(
                Result.error(r ->
                        r.setCode(403)
                                .setMessage("登录已失效")
                )
        );
    }
}
