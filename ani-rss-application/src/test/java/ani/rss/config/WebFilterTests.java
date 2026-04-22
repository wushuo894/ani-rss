package ani.rss.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebFilterTests {

    @Test
    void shouldNotRewriteMcpPath() throws Exception {
        WebFilter webFilter = new WebFilter();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/mcp");

        webFilter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        verify(request, never()).getRequestDispatcher(any());
    }

    @Test
    void shouldRewriteNonApiPathWithoutExtension() throws Exception {
        WebFilter webFilter = new WebFilter();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);

        when(request.getRequestURI()).thenReturn("/home");
        when(request.getRequestDispatcher("/home.html")).thenReturn(dispatcher);

        webFilter.doFilter(request, response, chain);

        verify(dispatcher, times(1)).forward(request, response);
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}
