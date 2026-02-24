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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        Global.REQUEST.set((HttpServletRequest) request);
        Global.RESPONSE.set((HttpServletResponse) response);
        try {
            filterChain.doFilter(request, response);
        } finally {
            Global.REQUEST.remove();
            Global.RESPONSE.remove();
        }
    }
}
