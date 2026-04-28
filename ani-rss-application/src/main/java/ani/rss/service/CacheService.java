package ani.rss.service;

import ani.rss.commons.GsonStatic;
import ani.rss.entity.Config;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.AfdianUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CacheService {

    /**
     * k: Bgm Id, v: Bgm Score
     *
     * @return
     */
    public JsonObject getBgmScore() {
        return getScore("bgm");
    }

    /**
     * k: Mikan Id, v: Bgm Score
     *
     * @return
     */
    public JsonObject getMikanScore() {
        return getScore("mikan");
    }

    public JsonObject getScore(String source) {
        if (!AfdianUtil.verifyExpirationTime()) {
            return new JsonObject();
        }
        Config config = ConfigUtil.CONFIG;
        String outTradeNo = config.getOutTradeNo();
        boolean tryOut = config.getTryOut();
        if (StrUtil.isBlank(outTradeNo) || tryOut) {
            return new JsonObject();
        }

        JsonObject jsonObject = new JsonObject();
        try {
            String url = StrFormatter.format(
                    "https://cache.wushuo.top/{}/score/{}",
                    source,
                    SecureUtil.sha256(outTradeNo)
            );
            jsonObject = HttpReq.get(url)
                    .timeout(1000 * 5)
                    .thenFunction(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            return new JsonObject();
                        }
                        HttpReq.assertStatus(res);
                        return GsonStatic.fromJson(res.body(), JsonObject.class);
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonObject;
    }

    public JsonObject getBgmCover() {
        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject = HttpReq.get("https://cache.wushuo.top/bgm/cover")
                    .thenFunction(res -> {
                        HttpReq.assertStatus(res);
                        return GsonStatic.fromJson(res.body(), JsonObject.class);
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonObject;
    }
}
