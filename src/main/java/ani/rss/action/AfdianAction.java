package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.util.AfdianUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;

/**
 * 爱发电
 */
@Auth
@Slf4j
@Path("/afdian")
public class AfdianAction implements BaseAction {
    @Override
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        String type = request.getParam("type");

        if (type.equals("verifyNo")) {
            Config config = getBody(Config.class);
            String outTradeNo = config.getOutTradeNo();
            JsonObject jsonObject = AfdianUtil.verifyNo(outTradeNo);
            int code = jsonObject.get("code").getAsInt();
            String message = jsonObject.get("message").getAsString();
            resultSuccess(result ->
                    result
                            .setCode(code)
                            .setMessage(message)
            );

            if (code == 200) {
                Long time = DateUtil.offsetYear(new Date(), 999).getTime();
                ConfigUtil.CONFIG.setOutTradeNo(outTradeNo)
                        .setExpirationTime(time)
                        .setTryOut(false);
                ConfigUtil.sync();
            }

            return;
        }

        if (type.equals("tryOut")) {
            Config config = getBody(Config.class);
            if (AfdianUtil.verifyExpirationTime()) {
                resultError(result ->
                        result
                                .setMessage("还在试用中!")
                );
                return;
            }

            String githubToken = config.getGithubToken();
            Assert.notBlank(githubToken, "GithubToken 不能为空");

            Boolean ok = HttpReq.get("https://api.github.com/user/starred/wushuo894/ani-rss")
                    .header("Authorization", "Bearer " + githubToken)
                    .thenFunction(HttpResponse::isOk);

            Assert.isTrue(ok, "未点击star");

            long time = DateUtil.offsetDay(new Date(), 15).getTime();
            ConfigUtil.CONFIG
                    .setGithubToken(githubToken)
                    .setExpirationTime(time)
                    .setTryOut(true);
            ConfigUtil.sync();
            resultSuccess(result ->
                    result
                            .setMessage("试用续期成功!")
                            .setData(time)
            );
        }

    }
}
