package ani.rss.util.basic;

import ani.rss.commons.ExceptionUtils;
import ani.rss.entity.Config;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
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
        String host = url.getHost();
        if (!"github.com".equals(host)) {
            return new HttpRequestPlus(url);
        }

        Config config = ConfigUtil.CONFIG;
        String github = config.getGithub();
        Boolean customGithub = config.getCustomGithub();
        String customGithubUrl = config.getCustomGithubUrl();

        if (customGithub) {
            if (StrUtil.isBlank(customGithubUrl)) {
                log.info("未填写自定义github加速");
                return new HttpRequestPlus(url);
            }
            github = customGithubUrl;
        }

        if (StrUtil.isBlank(github) || github.equals("None")) {
            return new HttpRequestPlus(url);
        }

        // 处理github加速
        String newUrl = github + "/" + url;
        log.info("github 已加速: {}", newUrl);
        return new HttpRequestPlus(UrlBuilder.ofHttp(newUrl, StandardCharsets.UTF_8));
    }

    public static HttpRequest of(String url) {
        // 去除分隔符重复
        url = url.replaceAll("(?<!https?:?)//", "/");
        return HttpRequestPlus.of(UrlBuilder.ofHttp(url, StandardCharsets.UTF_8));
    }

    public static HttpRequest of(String url, Charset charset) {
        // 去除分隔符重复
        url = url.replaceAll("(?<!https?:?)//", "/");
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
            String message = ExceptionUtils.getMessage(e);
            log.error("url: {}, error: {}", url, message);
            throw e;
        }
    }
}
