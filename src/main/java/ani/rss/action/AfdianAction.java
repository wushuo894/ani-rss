package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.entity.Result;
import ani.rss.util.AfdianUtil;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.date.DateUtil;
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
            result(
                    Result.success()
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
            Config config = ConfigUtil.CONFIG;
            if (AfdianUtil.verifyExpirationTime()) {
                result(
                        Result.error()
                                .setMessage("还在试用中!")
                );
                return;
            }
            long time = DateUtil.offsetDay(new Date(), 15).getTime();
            config.setExpirationTime(time)
                    .setTryOut(true);
            ConfigUtil.sync();
            result(
                    Result.success()
                            .setMessage("试用续期成功!")
                            .setData(time)
            );
        }

    }
}
