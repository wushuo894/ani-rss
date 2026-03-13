package ani.rss.config;

import ani.rss.entity.Global;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static ani.rss.controller.BaseController.setCacheControl;

@Component
public class WebFilter implements Filter {
    /**
     * 指定缓存的文件
     */
    private static final List<String> CACHE_EXT = List.of("css", "js", "jpg", "png", "svg", "ico");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        // 非 api
        if (!uri.startsWith("/api")) {
            String extName = FileUtil.extName(uri);

            if (StrUtil.isBlank(extName) && !uri.endsWith("/")) {
                String htmlPath = uri + ".html";
                request.getRequestDispatcher(htmlPath).forward(request, response);
                return;
            }

            if (StrUtil.isNotBlank(extName) && CACHE_EXT.contains(extName)) {
                setCacheControl(response, 86400);
            } else {
                setCacheControl(response, 0);
            }
        }

        Global.REQUEST.set(request);
        Global.RESPONSE.set(response);
        try {
            filterChain.doFilter(req, res);
        } finally {
            Global.REQUEST.remove();
            Global.RESPONSE.remove();
        }
    }
}
