package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.entity.*;
import ani.rss.enums.StringEnum;
import ani.rss.service.DownloadService;
import ani.rss.util.other.*;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@RestController
@RequestMapping
public class EmbyController extends BaseController {
    @Auth
    @Operation(summary = "获取媒体库")
    @PostMapping("/getEmbyViews")
    public Result<List<EmbyViews>> getEmbyViews(@RequestBody NotificationConfig notificationConfig) {
        List<EmbyViews> views = EmbyUtil.getViews(notificationConfig);
        return Result.success(views);
    }

    private static final ExecutorService EXECUTOR = ExecutorBuilder.create()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setWorkQueue(new LinkedBlockingQueue<>(256))
            .build();

    @Auth
    @Operation(summary = "BGM自动点格子")
    @PostMapping("/embyWebHook")
    public Result<Void> embyWebHook(@RequestBody EmbyWebHook embyWebHook) {
        log.debug("webhook: {}", embyWebHook);

        Config config = ConfigUtil.CONFIG;
        String bgmToken = config.getBgmToken();
        if (StrUtil.isBlank(bgmToken)) {
            log.info("bgmToken 为空");
            return Result.success();
        }

        String event = embyWebHook.getEvent();

        if (List.of("system.webhooktest", "system.notificationtest").contains(event)) {
            EmbyWebHook.Server server = embyWebHook.getServer();
            String id = server.getId();
            String name = server.getName();
            String version = server.getVersion();

            String s = """
                    接收到测试请求:
                    ====================================
                    IP: {}
                    ServerId: {}
                    ServerName: {}
                    ServerVersion: {}
                    ====================================
                    """;

            String ip = AuthUtil.getIp();
            log.info(s, ip, id, name, version);
            // 测试
            return Result.success();
        }

        EmbyWebHook.Item item = embyWebHook.getItem();

        String seriesName = item.getSeriesName();
        String fileName = item.getFileName();
        if (!ReUtil.contains(StringEnum.SEASON_REG, fileName)) {
            return Result.success();
        }
        // 季
        int season = Integer.parseInt(ReUtil.get(StringEnum.SEASON_REG, fileName, 1));

        // 番外
        if (season < 1) {
            return Result.success();
        }

        // 集 x.5
        double episode = Double.parseDouble(ReUtil.get(StringEnum.SEASON_REG, fileName, 2));
        if (ItemsUtil.is5(episode)) {
            return Result.success();
        }

        int type = getType(embyWebHook);

        if (type < 0) {
            // 播放状态未正确获取
            return Result.success();
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

        return Result.success();
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
