package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.util.BgmUtil;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Auth(value = false)
@Path("/web_hook")
public class WebHookAction implements BaseAction {
    @Override
    public synchronized void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        JsonObject body = getBody(JsonObject.class);

        log.debug("webhook: {}", body.toString());

        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            log.info("bgmToken 为空");
            response.sendOk();
            return;
        }

        JsonObject item = body.getAsJsonObject("Item");
        String seriesName = item.get("SeriesName").getAsString();
        String fileName = item.get("FileName").getAsString();
        String regStr = "S(\\d+)E(\\d+(\\.5)?)";
        if (!ReUtil.contains(regStr, fileName)) {
            response.sendOk();
            return;
        }
        int s = Integer.parseInt(ReUtil.get(regStr, fileName, 1));

        // 番外
        if (s < 1) {
            response.sendOk();
            return;
        }

        // x.5
        double e = Double.parseDouble(ReUtil.get(regStr, fileName, 2));
        if ((int) e == e - 0.5) {
            response.sendOk();
            return;
        }

        response.sendOk();

        int type = "item.markunplayed".equalsIgnoreCase(body.get("Event").getAsString()) ? 0 : 2;

        AtomicReference<String> seriesNameAtomic = new AtomicReference<>(seriesName);
        ThreadUtil.execute(new Runnable() {
            @Override
            public synchronized void run() {
                log.info("{} 标记为 [{}]", fileName, List.of("未看过", "想看", "看过").get(type));
                String episodeId = "";
                String subjectId = "";

                // 往后查两季 如果没有则停止
                for (int i = s; i <= s + 2; i++) {
                    if (i > 1) {
                        seriesNameAtomic.set(StrFormatter.format("{} 第{}季", seriesName, Convert.numberToChinese(i, false)));
                    }
                    subjectId = BgmUtil.getSubjectId(seriesNameAtomic.get());
                    episodeId = BgmUtil.getEpisodeId(subjectId, e);
                    if (StrUtil.isNotBlank(episodeId)) {
                        break;
                    }
                    ThreadUtil.sleep(1000L);
                }
                if (StrUtil.isBlank(episodeId)) {
                    log.info("获取bgm对应剧集失败");
                    return;
                }
                log.debug("subjectId: {}", subjectId);
                log.debug("episodeId: {}", episodeId);
                BgmUtil.collections(subjectId);
                BgmUtil.collectionsEpisodes(episodeId, type);
            }
        });
    }
}
