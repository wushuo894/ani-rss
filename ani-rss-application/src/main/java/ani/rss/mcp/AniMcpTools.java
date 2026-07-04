package ani.rss.mcp;

import ani.rss.controller.AniController;
import ani.rss.entity.*;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.exception.ResultException;
import ani.rss.mcp.dto.ListSubscriptionDTO;
import ani.rss.mcp.dto.SearchMikanDTO;
import ani.rss.mcp.vo.SubscriptionItemsPreviewVO;
import ani.rss.service.AniBTService;
import ani.rss.service.AnimeGardenService;
import ani.rss.service.MikanService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ItemsUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class AniMcpTools {

    @Resource
    private AniController aniController;

    @Resource
    private MikanService mikanService;

    @Resource
    private AniBTService aniBTService;

    @Resource
    private AnimeGardenService animeGardenService;

    @McpTool(
            name = "list_subscriptions",
            description = "列出现有 ANI-RSS 订阅，可按启用状态过滤",
            annotations = @McpTool.McpAnnotations(
                    title = "订阅列表",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public List<Ani> listSubscriptions(@McpToolParam ListSubscriptionDTO dto) {
        Boolean enabled = dto.getEnabled();
        return AniUtil.ANI_LIST.stream()
                .filter(ani -> Objects.isNull(enabled) || enabled.equals(ani.getEnable()))
                .toList();
    }

    @McpTool(
            name = "search_mikan",
            description = "按关键词搜索 Mikan 番剧，可传入季度条件",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 Mikan",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public Mikan searchMikan(@McpToolParam SearchMikanDTO dto) {
        String text = dto.getText();
        Mikan.Season season = dto.getSeason();
        text = ObjectUtil.defaultIfNull(text, "");
        season = ObjectUtil.defaultIfNull(season, new Mikan.Season());
        return mikanService.list(text, season);
    }

    @McpTool(
            name = "search_anibt",
            description = "搜索 AniBT 番剧",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 AniBT",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public AniBT searchAniBT() {
        return aniBTService.list("", "");
    }

    @McpTool(
            name = "search_anime_garden",
            description = "搜索 AnimeGarden 番剧列表",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 AnimeGarden",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public List<AnimeGarden.Week> searchAnimeGarden() {
        return animeGardenService.list("");
    }

    @McpTool(
            name = "get_mikan_groups",
            description = "根据 Mikan 番剧页面 URL 获取字幕组 RSS",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 Mikan 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public List<Mikan.Group> getMikanGroups(
            @McpToolParam(description = "蜜柑（Mikan）番剧页面 URL", required = true)
            String url
    ) {
        return mikanService.getGroups(url);
    }

    @McpTool(
            name = "get_anibt_groups",
            description = "根据 AniBT BGM ID 获取字幕组 RSS",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 AniBT 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public List<AniBT.Group> getAniBTGroups(
            @McpToolParam(description = "BGM 番剧 ID", required = true)
            String bgmId
    ) {
        return aniBTService.getGroups(bgmId);
    }

    @McpTool(
            name = "get_anime_garden_groups",
            description = "根据 AnimeGarden BGM ID 获取字幕组 RSS",
            annotations = @McpTool.McpAnnotations(
                    title = "获取 AnimeGarden 字幕组",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
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
            description = "预览某个 RSS 订阅最终会命中的原始剧集条目",
            annotations = @McpTool.McpAnnotations(
                    title = "预览订阅条目",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = true
            )
    )
    public SubscriptionItemsPreviewVO previewSubscriptionItems(@McpToolParam RssToAniDTO dto) {
        try {
            Ani ani = AniUtil.getAni(dto);
            List<Item> items = ItemsUtil.getItems(ani);
            return new SubscriptionItemsPreviewVO(ani, items);
        } catch (Exception e) {
            throw mcpException("RSS解析失败", e);
        }
    }

    @McpTool(
            name = "add_subscription",
            description = "添加一个 ANI-RSS 订阅。需要预览命中条目时请先调用 preview_subscription_items",
            annotations = @McpTool.McpAnnotations(
                    title = "添加订阅",
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = false,
                    openWorldHint = false
            )
    )
    public Ani addSubscription(@McpToolParam RssToAniDTO dto) {
        try {
            Ani ani = AniUtil.getAni(dto);
            aniController.addAni(ani);
            return ani;
        } catch (Exception e) {
            throw mcpException("创建订阅失败", e);
        }
    }

    private ResultException mcpException(String action, Exception e) {
        log.error(e.getMessage(), e);
        return ResultException.exception(action);
    }
}
