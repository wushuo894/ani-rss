package ani.rss.service;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.entity.web.Result;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.lang.Assert;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class AfdianService {
    /**
     * 检测爱发电订单
     *
     * @param no 订单号
     * @return 结果
     */
    public Result<Void> verifyNo(String no) {
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
     *
     * @return 是否有效
     */
    public Boolean verifyExpirationTime() {
        Config config = ConfigUtil.CONFIG;
        Long expirationTime = config.getExpirationTime();

        long time = new Date().getTime();
        return time < expirationTime;
    }
}
