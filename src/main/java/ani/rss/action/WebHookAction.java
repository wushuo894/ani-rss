package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Config;
import ani.rss.util.BgmUtil;
import ani.rss.util.ConfigUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Auth(value = false)
@Path("/web_hook")
public class WebHookAction implements BaseAction {
    @Override
    public synchronized void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            log.info("bgmToken 为空");
            response.sendOk();
            return;
        }

        JsonObject item = getBody(JsonObject.class).getAsJsonObject("Item");
        String seriesName = item.get("SeriesName").getAsString();
        String fileName = item.get("FileName").getAsString();
        String regStr = "S(\\d+)E(\\d+(\\.5)?)";
        if (!ReUtil.contains(regStr, fileName)) {
            response.sendOk();
            return;
        }
        int s = Integer.parseInt(ReUtil.get(regStr, fileName, 1));
        double e = Double.parseDouble(ReUtil.get(regStr, fileName, 2));
        if ((int) e == e - 0.5) {
            response.sendOk();
            return;
        }

        if (s > 1) {
            seriesName = StrFormatter.format("{} 第{}季", ReUtil.get(regStr, fileName, 1), Convert.numberToChinese(s, false));
        }

        log.info("打格子 {}", fileName);

        String subjectId = BgmUtil.getSubjectId(seriesName);
        BgmUtil.collections(subjectId);
        String episodeId = BgmUtil.getEpisodeId(subjectId, e);
        BgmUtil.collectionsEpisodes(episodeId);
    }
}
