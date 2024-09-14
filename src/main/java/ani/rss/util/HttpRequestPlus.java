package ani.rss.util;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpRequestPlus extends HttpRequest {
    public HttpRequestPlus(UrlBuilder url) {
        super(url);
    }

    @Override
    public HttpResponse execute(boolean isAsync) {
        String url = getUrl();
        try {
            return super.execute(isAsync);
        } catch (Exception e) {
            log.error("{} {}", url, e.getMessage());
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
