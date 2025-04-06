package ani.rss.util;

import ani.rss.entity.Config;
import cn.hutool.core.lang.Assert;
import com.google.gson.JsonObject;

import java.util.Date;

public class AfdianUtil {
    /**
     * 检测爱发电订单
     *
     * @param no 订单号
     * @return
     */
    public static JsonObject verifyNo(String no) {
        Assert.notBlank(no, "订单号为空");
        return HttpReq.post("https://afdian.wushuo.top?out_trade_no=" + no)
                .thenFunction(res ->
                        GsonStatic.fromJson(res.body(), JsonObject.class)
                );
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
}
