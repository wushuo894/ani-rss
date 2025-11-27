package ani.rss.action;

import ani.rss.web.action.BaseAction;
import ani.rss.web.annotation.Auth;
import ani.rss.web.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.ProxyTest;
import ani.rss.entity.Result;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * 代理
 */
@Slf4j
@Auth
@Path("/proxy")
public class ProxyAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String url = request.getParam("url");
        Config config = getBody(Config.class);
        url = Base64.decodeStr(url);

        log.info(url);

        HttpRequest httpRequest = HttpReq.get(url);
        HttpReq.setProxy(httpRequest, config);

        ProxyTest proxyTest = new ProxyTest();
        Result<ProxyTest> result = Result.success(proxyTest);

        long start = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        try {
            httpRequest
                    .then(res -> {
                        int status = res.getStatus();
                        proxyTest.setStatus(status);

                        String title = Jsoup.parse(res.body())
                                .title();
                        result.setMessage(StrFormatter.format("测试成功 {}", title));
                    });
        } catch (Exception e) {
            result.setMessage(e.getMessage())
                    .setCode(HttpStatus.HTTP_INTERNAL_ERROR);
        }

        long end = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        proxyTest.setTime(end - start);
        result(result);
    }
}
