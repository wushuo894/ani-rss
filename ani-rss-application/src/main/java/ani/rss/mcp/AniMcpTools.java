package ani.rss.mcp;

import ani.rss.commons.GsonStatic;
import ani.rss.controller.AniController;
import ani.rss.entity.*;
import ani.rss.entity.dto.AniBTQueryDTO;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.exception.ResultException;
import ani.rss.mcp.dto.DeleteSubscriptionDTO;
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
import java.util.Map;
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
                    idempotentHint = true
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
                    idempotentHint = true
            )
    )
    public AniBT searchAniBT() {
        return aniBTService.list(new AniBTQueryDTO());
    }

    @McpTool(
            name = "search_anime_garden",
            description = "搜索 AnimeGarden 番剧列表",
            annotations = @McpTool.McpAnnotations(
                    title = "搜索 AnimeGarden",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
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
                    idempotentHint = true
            )
    )
    public List<Mikan.Group> getMikanGroups(
            @McpToolParam(description = "蜜柑（Mikan）番剧页面 URL")
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
                    idempotentHint = true
            )
    )
    public List<AniBT.Group> getAniBTGroups(
            @McpToolParam(description = "BGM 番剧 ID")
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
                    idempotentHint = true
            )
    )
    public List<AnimeGarden.Group> getAnimeGardenGroups(
            @McpToolParam(description = "BGM 番剧 ID")
            String bgmId
    ) {
        return animeGardenService.group(bgmId);
    }

    /**
     * 将 RSS 参数解析为完整订阅对象，供预览和添加工具继续使用。
     *
     * @param dto RSS 订阅参数
     * @return 已补全番剧信息的订阅
     */
    @McpTool(
            name = "rss_to_subscription",
            description = "将 RSS 地址解析并转换为完整的 ANI-RSS 订阅对象。请先调用此工具，再将返回的订阅对象传给 preview_subscription_items 或 add_subscription",
            annotations = @McpTool.McpAnnotations(
                    title = "RSS 转订阅",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public Ani rssToSubscription(@McpToolParam RssToAniDTO dto) {
        try {
            return AniUtil.getAni(dto);
        } catch (Exception e) {
            throw mcpException("RSS转换失败", e);
        }
    }

    @McpTool(
            name = "preview_subscription_items",
            description = "预览完整订阅对象最终会命中的原始剧集条目。请先调用 rss_to_subscription",
            annotations = @McpTool.McpAnnotations(
                    title = "预览订阅条目",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SubscriptionItemsPreviewVO previewSubscriptionItems(
            @McpToolParam(description = "由 rss_to_subscription 返回的完整订阅对象")
            Map<String, Object> subscription
    ) {
        try {
            Ani ani = mapToAni(subscription);
            List<Item> items = ItemsUtil.getItems(ani);
            return new SubscriptionItemsPreviewVO(ani, items);
        } catch (Exception e) {
            throw mcpException("RSS解析失败", e);
        }
    }

    @McpTool(
            name = "add_subscription",
            description = "添加一个完整的 ANI-RSS 订阅对象。请先调用 rss_to_subscription，可选调用 preview_subscription_items",
            annotations = @McpTool.McpAnnotations(
                    title = "添加订阅",
                    destructiveHint = false,
                    openWorldHint = false
            )
    )
    public Ani addSubscription(
            @McpToolParam(description = "由 rss_to_subscription 返回的完整订阅对象")
            Map<String, Object> subscription
    ) {
        try {
            Ani ani = mapToAni(subscription);
            aniController.addAni(ani, false);
            return ani;
        } catch (Exception e) {
            throw mcpException("创建订阅失败", e);
        }
    }

    /**
     * 使用完整订阅对象更新已有订阅。
     *
     * @param subscription 从订阅列表取得并修改后的完整订阅字段
     * @return 修改后的订阅
     */
    @McpTool(
            name = "edit_subscription",
            description = "编辑一个 ANI-RSS 订阅。请先调用 list_subscriptions 获取完整订阅对象，保留 id 并修改需要调整的字段后传入",
            annotations = @McpTool.McpAnnotations(
                    title = "编辑订阅",
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false
            )
    )
    public Ani editSubscription(
            @McpToolParam(description = "修改后的完整订阅对象，id 必须对应现有订阅")
            Map<String, Object> subscription
    ) {
        try {
            // 使用 Map 接收订阅，避免实体空字段被 MCP Schema 声明为必填。
            Ani ani = mapToAni(subscription);
            aniController.setAni(ani);
            return ani;
        } catch (Exception e) {
            throw mcpException("编辑订阅失败", e);
        }
    }

    /**
     * 删除指定订阅，可选择是否一并删除下载数据。
     *
     * @param dto 删除参数
     * @return 已删除的订阅 ID
     */
    @McpTool(
            name = "delete_subscription",
            description = "删除一个或多个 ANI-RSS 订阅。deleteFiles 为 true 时还会删除下载器任务和本地视频文件",
            annotations = @McpTool.McpAnnotations(
                    title = "删除订阅",
                    openWorldHint = false
            )
    )
    public List<String> deleteSubscription(@McpToolParam DeleteSubscriptionDTO dto) {
        try {
            List<String> ids = dto.getIds();
            boolean deleteFiles = Boolean.TRUE.equals(dto.getDeleteFiles());
            aniController.deleteAni(ids, deleteFiles);
            return ids;
        } catch (Exception e) {
            throw mcpException("删除订阅失败", e);
        }
    }

    private Ani mapToAni(Map<String, Object> map) {
        return GsonStatic.fromJson(GsonStatic.toJson(map), Ani.class);
    }

    private ResultException mcpException(String action, Exception e) {
        log.error(e.getMessage(), e);
        return ResultException.exception(action);
    }
}
