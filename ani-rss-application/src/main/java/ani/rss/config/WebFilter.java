package ani.rss.config;

import ani.rss.entity.Global;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        if (!uri.startsWith("/api") && !uri.contains(".") && !uri.equals("/")) {
            String htmlPath = uri + ".html";
            request.getRequestDispatcher(htmlPath).forward(request, response);
            return;
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
