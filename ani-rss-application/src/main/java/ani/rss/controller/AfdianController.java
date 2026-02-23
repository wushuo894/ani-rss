package ani.rss.controller;

import ani.rss.entity.Config;
import ani.rss.entity.Result;
import ani.rss.entity.TryOut;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AfdianUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.http.HttpResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Hidden
@RestController
public class AfdianController {
    @PostMapping("/verifyNo")
    public Result<Void> verifyNo(@RequestBody Config config) {
        String outTradeNo = config.getOutTradeNo();
        Result<Void> result = AfdianUtil.verifyNo(outTradeNo);

        int code = result.getCode();
        if (code == 200) {
            Long time = DateUtil.offsetYear(new Date(), 999).getTime();
            ConfigUtil.CONFIG.setOutTradeNo(outTradeNo)
                    .setExpirationTime(time)
                    .setTryOut(false);
            ConfigUtil.sync();
        }

        return result;
    }

    @PostMapping("/tryOut")
    public Result<Long> tryOut(@RequestBody Config config) {
        if (AfdianUtil.verifyExpirationTime()) {
            return Result.error("还在试用中!");
        }

        String githubToken = config.getGithubToken();
        Assert.notBlank(githubToken, "GithubToken 不能为空");

        Boolean ok = HttpReq.get("https://api.github.com/user/starred/wushuo894/ani-rss")
                .header("Authorization", "Bearer " + githubToken)
                .thenFunction(HttpResponse::isOk);

        Assert.isTrue(ok, "未点击star");

        TryOut tryOut = AfdianUtil.getTryOut();

        Boolean enable = tryOut.getEnable();
        Boolean renewal = tryOut.getRenewal();
        Integer day = tryOut.getDay();
        String message = tryOut.getMessage();

        Assert.isTrue(enable, message);

        if (config.getTryOut()) {
            // 已经有过试用
            Assert.isTrue(renewal, message);
        }

        long time = DateUtil.offsetDay(new Date(), day).getTime();
        ConfigUtil.CONFIG
                .setGithubToken(githubToken)
                .setExpirationTime(time)
                .setTryOut(true);
        ConfigUtil.sync();
        return Result.success(time).setMessage(message);
    }
}
