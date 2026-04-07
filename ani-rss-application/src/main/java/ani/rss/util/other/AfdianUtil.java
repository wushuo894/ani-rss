package ani.rss.util.other;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.entity.TryOut;
import ani.rss.entity.web.Result;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.lang.Assert;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 你可以修改后自用, 但请仅存放于 私人仓库、私人docker镜像
 */
@Slf4j
public class AfdianUtil {
    /**
     * 检测爱发电订单
     * <p>
     * 你可以修改后自用, 但请仅存放于 私人仓库、私人docker镜像
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
     * 你可以修改后自用, 但请仅存放于 私人仓库、私人docker镜像
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
}
