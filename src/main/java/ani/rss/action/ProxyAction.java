package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.util.HttpReq;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;

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

        HttpRequest httpRequest = HttpReq.get(url, false);
        HttpReq.setProxy(httpRequest, config);

        long start = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());
        Integer status = httpRequest
                .thenFunction(HttpResponse::getStatus);

        long end = LocalDateTimeUtil.toEpochMilli(LocalDateTimeUtil.now());

        resultSuccess(Map.of("status", status, "time", end - start));
    }
}
