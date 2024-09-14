package ani.rss.util;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRequestPlus extends HttpRequest {
    public HttpRequestPlus(UrlBuilder url) {
        super(url);
    }

    public static HttpRequest of(UrlBuilder url) {
        return new HttpRequestPlus(url);
    }

    public static HttpRequest of(String url) {
        return HttpRequestPlus.of(UrlBuilder.ofHttp(url, StandardCharsets.UTF_8));
    }

    public static HttpRequest of(String url, Charset charset) {
        return HttpRequestPlus.of(UrlBuilder.ofHttp(url, charset));
    }

    public static HttpRequest get(String url) {
        return HttpRequestPlus.of(url).method(Method.GET);
    }

    public static HttpRequest post(String url) {
        return HttpRequestPlus.of(url).method(Method.POST);
    }

    @Override
    public HttpResponse execute(boolean isAsync) {
        String url = getUrl();
        try {
            return super.execute(isAsync);
        } catch (Exception e) {
            log.error("url ===> {}, error ===> {}", url, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
