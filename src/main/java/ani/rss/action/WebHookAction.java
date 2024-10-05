package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.util.AniUtil;
import ani.rss.util.BgmUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ObjectUtil;
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
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Auth(type = AuthType.API_KEY)
@Path("/web_hook")
public class WebHookAction implements BaseAction {

    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(32))
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
        EXECUTOR.execute(() -> {
            log.info("{} 标记为 [{}]", fileName, List.of("未看过", "想看", "看过").get(type));
            String episodeId;
            String subjectId;
            List<Ani> anis = ObjectUtil.clone(AniUtil.ANI_LIST);
            Optional<String> first = anis.stream()
                    .filter(ani -> {
                        List<File> downloadPath = TorrentUtil.getDownloadPath(ani);
                        for (File file : downloadPath) {
                            if (!file.exists()) {
                                continue;
                            }
                            String parent = new File(path).getParent();
                            if (file.toString().equals(parent)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .map(BgmUtil::getSubjectId)
                    .findFirst();

            if (first.isPresent()) {
                subjectId = first.get();
            } else {
                if (s > 1) {
                    seriesNameAtomic.set(StrFormatter.format("{} 第{}季", seriesName, Convert.numberToChinese(s, false)));
                }
                subjectId = BgmUtil.getSubjectId(seriesNameAtomic.get());
            }

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
