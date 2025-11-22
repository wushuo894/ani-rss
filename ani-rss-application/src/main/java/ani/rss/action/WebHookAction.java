package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.auth.enums.AuthType;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.EmbyWebHook;
import ani.rss.entity.tmdb.Tmdb;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.*;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
        String body = getBody();
        log.debug("webhook: {}", body);

        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            log.info("bgmToken 为空");
            response.sendOk();
            return;
        }

        EmbyWebHook embyWebHook = GsonStatic.fromJson(body, EmbyWebHook.class);

        EmbyWebHook.Item item = embyWebHook.getItem();

        String seriesName = item.getSeriesName();
        String fileName = item.getFileName();
        if (!ReUtil.contains(StringEnum.SEASON_REG, fileName)) {
            response.sendOk();
            return;
        }
        // 季
        int season = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, fileName, 1));

        // 番外
        if (season < 1) {
            response.sendOk();
            return;
        }

        // 集 x.5
        double episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, fileName, 2));
        if (ItemsUtil.is5(episode)) {
            response.sendOk();
            return;
        }

        response.sendOk();

        int type = getType(embyWebHook);

        if (type < 0) {
            // 播放状态未正确获取
            return;
        }

        EXECUTOR.execute(() -> {
            log.info("{} 标记为 [{}]", fileName, List.of("未看过", "想看", "看过").get(type));
            List<Ani> anis = AniUtil.ANI_LIST;

            // 优先匹配路径相同的
            String subjectId = anis.stream()
                    .filter(ani -> embyEqualsAni(embyWebHook, ani))
                    .map(BgmUtil::getSubjectId)
                    .findFirst()
                    .orElseGet(() -> BgmUtil.getSubjectId(seriesName, season));

            String episodeId = BgmUtil.getEpisodeId(subjectId, episode);

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

    /**
     * 是否匹配到订阅
     *
     * @param ani
     * @return
     */
    public Boolean embyEqualsAni(EmbyWebHook embyWebHook, Ani ani) {
        EmbyWebHook.Item item = embyWebHook.getItem();
        String fileName = item.getFileName();
        if (!ReUtil.contains(StringEnum.SEASON_REG, fileName)) {
            return false;
        }

        // 季
        int season = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, fileName, 1));

        if (season != ani.getSeason()) {
            return false;
        }

        String bgmUrl = ani.getBgmUrl();
        if (StrUtil.isBlank(bgmUrl)) {
            // bgmUrl为空
            return false;
        }

        String path = item.getPath();
        String parent = new File(path).getParent();
        String downloadPath = DownloadService.getDownloadPath(ani);
        if (downloadPath.equals(parent)) {
            // 路径相同
            return true;
        }

        String title = ani.getTitle();
        title = RenameUtil.renameDel(title, false);
        String seriesName = item.getSeriesName();
        if (title.equals(seriesName)) {
            // 名称与季相同
            return true;
        }

        Tmdb tmdb = ani.getTmdb();
        if (Objects.isNull(tmdb)) {
            return false;
        }

        // 对比tmdb名称
        String name = tmdb.getName();
        if (StrUtil.isNotBlank(name)) {
            if (name.equals(seriesName)) {
                return true;
            }
        }

        // 对比tmdb原名
        String originalName = tmdb.getOriginalName();
        if (StrUtil.isNotBlank(originalName)) {
            return originalName.equals(seriesName);
        }
        return false;
    }

    /**
     * 获取播放状态
     *
     * @param embyWebHook
     * @return
     */
    private static Integer getType(EmbyWebHook embyWebHook) {
        String event = embyWebHook.getEvent();

        if ("item.markunplayed".equalsIgnoreCase(event)) {
            // 标记未看
            return 0;
        }

        if ("item.markplayed".equalsIgnoreCase(event)) {
            // 已看
            return 2;
        }

        if ("playback.stop".equalsIgnoreCase(event)) {
            boolean playedToCompletion = embyWebHook.getPlaybackInfo()
                    .getPlayedToCompletion();
            if (playedToCompletion) {
                // 已看
                return 2;
            }
        }
        return -1;
    }
}
