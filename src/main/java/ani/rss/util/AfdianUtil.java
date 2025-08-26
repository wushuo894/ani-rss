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

/**
 * 请 "盗版者" 存放于私人仓库 私人docker镜像
 * 请 "盗版者" 存放于私人仓库 私人docker镜像
 * 请 "盗版者" 存放于私人仓库 私人docker镜像
 */
@Slf4j
public class AfdianUtil {
    /**
     * 检测爱发电订单
     * <p>
     * 请 "盗版者" 存放于私人仓库 私人docker镜像
     *
     * @param no 订单号
     * @return
     */
    public static Result<Void> verifyNo(String no) {
        Assert.notBlank(no, "订单号为空");
        return HttpReq.post("https://afdian.wushuo.top?out_trade_no=" + no)
                .timeout(1000 * 5)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    Result<Void> result = new Result<>();
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    result.setMessage(jsonObject.get("message").getAsString());
                    result.setCode(jsonObject.get("code").getAsInt());
                    return result;
                });
    }

    /**
     * 捐赠是否有效
     * <p>
     * 请 "盗版者" 存放于私人仓库 私人docker镜像
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
        return HttpReq.get("https://docs.wushuo.top/TryOut.json")
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return GsonStatic.fromJson(res.body(), TryOut.class);
                });
    }

    /**
     * 校验捐赠信息
     * <p>
     * 请 "盗版者" 存放于私人仓库 私人docker镜像
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
