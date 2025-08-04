package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.enums.StringEnum;
import ani.rss.util.AniUtil;
import ani.rss.util.BgmUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.google.gson.JsonObject;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebHook
 */
@Slf4j
@Auth(type = AuthType.API_KEY)
@Path("/web_hook")
public class WebHookAction implements BaseAction {

    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    @Override
    @Synchronized("EXECUTOR")
    public void doAction(HttpServerRequest request, HttpServerResponse response) throws IOException {
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
        String path = item.get("Path").getAsString();
        String parent = new File(path).getParent();
        String seriesName = item.get("SeriesName").getAsString();
        String fileName = item.get("FileName").getAsString();
        if (!ReUtil.contains(StringEnum.SEASON_REG, fileName)) {
            response.sendOk();
            return;
        }
        int s = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, fileName, 1));

        // 番外
        if (s < 1) {
            response.sendOk();
            return;
        }

        // x.5
        double e = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, fileName, 2));
        if ((int) e == e - 0.5) {
            response.sendOk();
            return;
        }

        response.sendOk();

        int type = "item.markunplayed".equalsIgnoreCase(body.get("Event").getAsString()) ? 0 : 2;

        EXECUTOR.execute(() -> {
            log.info("{} 标记为 [{}]", fileName, List.of("未看过", "想看", "看过").get(type));
            String episodeId;
            String subjectId;
            List<Ani> anis = AniUtil.ANI_LIST;

            // 优先匹配路径相同的
            Optional<String> first = anis.stream()
                    .filter(ani -> {
                        String bgmUrl = ani.getBgmUrl();
                        if (StrUtil.isBlank(bgmUrl)) {
                            return false;
                        }
                        File downloadPath = TorrentUtil.getDownloadPath(ani);
                        return downloadPath.toString().equals(parent);
                    })
                    .map(BgmUtil::getSubjectId)
                    .findFirst();

            if (first.isEmpty()) {
                // 匹配名称相同的
                first = anis.stream()
                        .filter(ani -> {
                            String bgmUrl = ani.getBgmUrl();
                            if (StrUtil.isBlank(bgmUrl)) {
                                return false;
                            }
                            String title = ani.getTitle();
                            title = title.replaceAll(StringEnum.YEAR_REG, "")
                                    .trim();
                            Integer season = ani.getSeason();
                            return title.equals(seriesName) && s == season;
                        })
                        .map(BgmUtil::getSubjectId)
                        .findFirst();
            }

            subjectId = first.orElseGet(() -> BgmUtil.getSubjectId(seriesName, s));
            episodeId = BgmUtil.getEpisodeId(subjectId, e);

            if (StrUtil.isBlank(episodeId)) {
                log.info("获取bgm对应剧集失败");
                return;
            }
            log.debug("subjectId: {}", subjectId);
            log.debug("episodeId: {}", episodeId);
            BgmUtil.collections(subjectId);
            BgmUtil.collectionsEpisodes(episodeId, type);
        });
    }
}
