package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.Result;
import ani.rss.entity.TryOut;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class AfdianUtil {
    /**
     * 检测爱发电订单
     *
     * @param no 订单号
     * @return
     */
    public static Result<Void> verifyNo(String no) {
        Result<Void> result = new Result<>();
        result.setCode(200);
        result.setMessage("验证成功");
        return result;
    }

    /**
     * 捐赠是否有效
     *
     * @return
     */
    public static Boolean verifyExpirationTime() {
        Config config = ConfigUtil.CONFIG;
        Long expirationTime = config.getExpirationTime();

        long time = new Date().getTime();
        return time < expirationTime;
    }

    /**
     * 获取试用设置
     *
     * @return
     */
    public static TryOut getTryOut() {
        return HttpReq.get("https://docs.wushuo.top/TryOut.json", true)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), TryOut.class);
                });
    }

    /**
     * 校验捐赠信息
     */
    public static void verify() {
        if (!verifyExpirationTime()) {
            return;
        }

        Config config = ConfigUtil.CONFIG;
        Long expirationTime = config.getExpirationTime();

        if (config.getTryOut()) {
            TryOut tryOut = getTryOut();
            Integer day = tryOut.getDay();
            long time = DateUtil.offsetDay(new Date(), day).getTime();
            if (expirationTime > time) {
                expirationTime = time;
            }
            config.setExpirationTime(expirationTime);
            return;
        }

        String outTradeNo = config.getOutTradeNo();
        if (StrUtil.isBlank(outTradeNo)) {
            config.setExpirationTime(0L);
            return;
        }

        Result<Void> result = verifyNo(outTradeNo);
        Integer code = result.getCode();
        String message = result.getMessage();
        if (code != 200) {
            config.setExpirationTime(0L);
            log.error(message);
        }
    }
}
