package ani.rss.mcp;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.GroupRegexUtils;
import ani.rss.dto.RssToAniDTO;
import ani.rss.entity.*;
import ani.rss.exception.ResultException;
import ani.rss.service.AniBTService;
import ani.rss.service.AnimeGardenService;
import ani.rss.service.DownloadService;
import ani.rss.service.MikanService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.ItemsUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.thread.ThreadUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AniMcpTools {

    @Resource
    private MikanService mikanService;

    @Resource
    private AniBTService aniBTService;

    @Resource
    private AnimeGardenService animeGardenService;

    @Resource
    private DownloadService downloadService;

    @McpTool(
            name = "list_subscriptions",
            description = "列出现有 ANI-RSS 订阅，可按启用状态过滤。",
            annotations = @McpTool.McpAnnotations(
                    title = "订阅列表",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public List<Ani> listSubscriptions(
            @McpToolParam(description = "可选的启用状态过滤：true 只返回启用订阅，false 只返回禁用订阅，不传则返回全部", required = false)
            Boolean enabled
    ) {
        return AniUtil.ANI_LIST.stream()
                .filter(ani -> enabled == null || enabled.equals(ani.getEnable()))
                .toList();
    }

    @McpTool(
            name = "search_mikan",
            description = "按关键词搜索 Mikan 番剧，可传入季度条件。",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 Mikan",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public Mikan searchMikan(
            @McpToolParam(description = "搜索关键词", required = true)
            String text,
            @McpToolParam(description = "可选季度过滤，例如年份和季度", required = false)
            Mikan.Season season
    ) {
        return mikanService.list(text, season);
    }

    @McpTool(
            name = "search_anibt",
            description = "搜索 AniBT 番剧，可按季度或 BGM 地址过滤。",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 AniBT",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public AniBT searchAniBT(
            @McpToolParam(description = "季度标识，例如 2026-04；不传则使用 AniBT 默认季度", required = false)
            String season,
            @McpToolParam(description = "可选 BGM 番剧地址，用于定位单个番剧", required = false)
            String bgmUrl
    ) {
        return aniBTService.list(season, bgmUrl);
    }

    @McpTool(
            name = "search_anime_garden",
            description = "搜索 AnimeGarden 番剧列表，可传入 BGM 地址定位单个番剧。",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 AnimeGarden",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<AnimeGarden.Week> searchAnimeGarden(
            @McpToolParam(description = "可选 BGM 番剧地址，用于定位单个番剧；不传则返回 AnimeGarden 当前列表", required = false)
            String bgmUrl
    ) {
        return animeGardenService.list(bgmUrl);
    }

    @McpTool(
            name = "get_mikan_groups",
            description = "根据 Mikan 番剧页面 URL 获取字幕组 RSS。",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 Mikan 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<Mikan.Group> getMikanGroups(
            @McpToolParam(description = "蜜柑（Mikan）番剧页面 URL", required = true)
            String url
    ) {
        List<Mikan.Group> groups = mikanService.getGroups(url);
        for (Mikan.Group group : groups) {
            List<Mikan.Item> items = group.getItems();
            GroupRegex groupRegx = GroupRegexUtils.toGroupRegx(items, Mikan.Item::getTitle);
            group.setGroupRegex(groupRegx);
        }
        return groups;
    }

    @McpTool(
            name = "get_anibt_groups",
            description = "根据 AniBT BGM ID 获取字幕组 RSS。",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 AniBT 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<AniBT.Group> getAniBTGroups(
            @McpToolParam(description = "BGM 番剧 ID", required = true)
            String bgmId
    ) {
        List<AniBT.Group> groups = aniBTService.getGroups(bgmId);
        for (AniBT.Group group : groups) {
            List<AniBT.Item> items = group.getItems();
            GroupRegex groupRegx = GroupRegexUtils.toGroupRegx(items, AniBT.Item::getTitle);

            group.setBgmId(bgmId)
                    .setGroupRegex(groupRegx);
        }
        return groups;
    }

    @McpTool(
            name = "get_anime_garden_groups",
            description = "根据 AnimeGarden BGM ID 获取字幕组 RSS。",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 AnimeGarden 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<AnimeGarden.Group> getAnimeGardenGroups(
            @McpToolParam(description = "BGM 番剧 ID")
            String bgmId
    ) {
        return animeGardenService.group(bgmId);
    }

    @McpTool(
            name = "preview_subscription_items",
            description = "预览某个 RSS 订阅最终会命中的原始剧集条目，不添加订阅。",
            annotations = @McpTool.McpAnnotations(
                    title = "预览订阅条目",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SubscriptionItemsPreview previewSubscriptionItems(
            @McpToolParam(description = "订阅预览请求，包含 RSS 地址 url、类型 type、字幕组 subgroup、BGM 地址 bgmUrl 等字段", required = true)
            RssToAniDTO request
    ) {
        try {
            Ani ani = AniUtil.getAni(request);
            return new SubscriptionItemsPreview(ani, ItemsUtil.getItems(ani));
        } catch (Exception e) {
            throw mcpException("RSS解析失败", e);
        }
    }

    @McpTool(
            name = "create_subscription",
            description = "根据 RSS 请求创建一个 ANI-RSS 订阅。需要预览命中条目时请先调用 preview_subscription_items。",
            annotations = @McpTool.McpAnnotations(
                    title = "创建订阅",
                    destructiveHint = false
            )
    )
    public Ani createSubscription(
            @McpToolParam(description = "订阅创建请求，包含 RSS 地址 url、类型 type、字幕组 subgroup、BGM 地址 bgmUrl 等字段", required = true)
            RssToAniDTO request,
            @McpToolParam(description = "创建后是否启用订阅", required = false)
            Boolean enable
    ) {
        try {
            Ani ani = AniUtil.getAni(request);
            if (enable != null) {
                ani.setEnable(enable);
            }
            return addAni(ani);
        } catch (Exception e) {
            throw mcpException("创建订阅失败", e);
        }
    }

    private Ani addAni(Ani ani) {
        ani.setTitle(ani.getTitle().trim())
                .setUrl(ani.getUrl().trim());
        AniUtil.verify(ani);

        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isPresent()) {
            throw new IllegalArgumentException("此订阅已存在");
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();
        if (first.isPresent()) {
            Config config = ConfigUtil.CONFIG;
            if (config.getReplace()) {
                AniUtil.ANI_LIST.remove(first.get());
                log.info("自动替换 {} 第{}季", ani.getTitle(), ani.getSeason());
            } else {
                throw new IllegalArgumentException("订阅标题重复");
            }
        }

        AniUtil.ANI_LIST.add(ani);
        AniUtil.sync();
        if (ani.getEnable()) {
            ThreadUtil.execute(() -> {
                if (TorrentUtil.login()) {
                    downloadService.downloadAni(ani);
                }
            });
        } else {
            ThreadUtil.execute(() -> {
                try {
                    List<Item> items = ItemsUtil.getItems(ani);
                    int currentEpisodeNumber = ItemsUtil.currentEpisodeNumber(ani, items);
                    ani.setCurrentEpisodeNumber(currentEpisodeNumber);
                } catch (Exception e) {
                    log.error(ExceptionUtils.getMessage(e), e);
                }
            });
        }
        log.info("添加订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());
        return ani;
    }

    private RuntimeException mcpException(String action, Exception e) {
        String message = e instanceof ResultException resultException
                ? resultException.getResult().getMessage()
                : ExceptionUtils.getMessage(e);
        log.error("{}: {}", action, message, e);
        return new IllegalArgumentException(action + ": " + message, e);
    }

    public record SubscriptionItemsPreview(
            Ani subscription,
            List<Item> items
    ) {
    }
}
