package ani.rss.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class McpCatalog {
    public static final String GENERIC_TOOL_NAME = "ani_rss.api_call";

    private static final List<McpEndpoint> ENDPOINTS = List.of(
            ep("config", "config", "/api/config", "POST", false, false, "Get current Ani-RSS config"),
            ep("setConfig", "config", "/api/setConfig", "POST", false, false, "Update Ani-RSS config"),
            ep("clearCache", "config", "/api/clearCache", "POST", false, false, "Clear runtime cache"),
            ep("trackersUpdate", "config", "/api/trackersUpdate", "POST", false, false, "Refresh trackers from config"),
            ep("testProxy", "config", "/api/testProxy", "POST", false, false, "Test configured proxy"),
            ep("downloadLoginTest", "config", "/api/downloadLoginTest", "POST", false, false, "Validate downloader login"),
            ep("customJs", "config", "/api/custom.js", "GET", false, true, "Fetch custom JavaScript"),
            ep("customCss", "config", "/api/custom.css", "GET", false, true, "Fetch custom CSS"),
            ep("exportConfig", "config", "/api/exportConfig", "GET", false, true, "Export configuration archive"),
            ep("importConfig", "config", "/api/importConfig", "POST", true, false, "Import configuration archive"),

            ep("listAni", "anime", "/api/listAni", "POST", false, false, "List subscriptions"),
            ep("addAni", "anime", "/api/addAni", "POST", false, false, "Create subscription"),
            ep("setAni", "anime", "/api/setAni", "POST", false, false, "Update subscription"),
            ep("deleteAni", "anime", "/api/deleteAni", "POST", false, false, "Delete subscriptions"),
            ep("updateTotalEpisodeNumber", "anime", "/api/updateTotalEpisodeNumber", "POST", false, false, "Refresh episode totals"),
            ep("batchEnable", "anime", "/api/batchEnable", "POST", false, false, "Batch enable/disable subscriptions"),
            ep("refreshAll", "anime", "/api/refreshAll", "POST", false, false, "Refresh all subscriptions"),
            ep("refreshAni", "anime", "/api/refreshAni", "POST", false, false, "Refresh one subscription"),
            ep("rssToAni", "anime", "/api/rssToAni", "POST", false, false, "Convert RSS payload to Ani object"),
            ep("previewAni", "anime", "/api/previewAni", "POST", false, false, "Preview matching results"),
            ep("downloadPath", "anime", "/api/downloadPath", "POST", false, false, "Resolve download path"),
            ep("importAni", "anime", "/api/importAni", "POST", false, false, "Import subscriptions"),
            ep("refreshCover", "anime", "/api/refreshCover", "POST", false, false, "Refresh cover for one subscription"),

            ep("mikan", "search", "/api/mikan", "POST", false, false, "Search Mikan"),
            ep("mikanGroup", "search", "/api/mikanGroup", "POST", false, false, "Fetch Mikan subgroup list"),
            ep("mikanCover", "search", "/api/mikanCover", "GET", false, true, "Fetch Mikan cover image"),
            ep("getThemoviedbName", "search", "/api/getThemoviedbName", "POST", false, false, "Resolve TMDB title"),
            ep("getThemoviedbGroup", "search", "/api/getThemoviedbGroup", "POST", false, false, "Fetch TMDB episode groups"),

            ep("searchBgm", "bgm", "/api/searchBgm", "POST", false, false, "Search Bangumi subject"),
            ep("getAniBySubjectId", "bgm", "/api/getAniBySubjectId", "POST", false, false, "Convert Bangumi subject to Ani object"),
            ep("getBgmTitle", "bgm", "/api/getBgmTitle", "POST", false, false, "Get Bangumi title"),
            ep("rate", "bgm", "/api/rate", "POST", false, false, "Read Bangumi rating"),
            ep("setRate", "bgm", "/api/setRate", "POST", false, false, "Submit Bangumi rating"),
            ep("meBgm", "bgm", "/api/meBgm", "POST", false, false, "Get Bangumi account details"),
            ep("bgmOauthCallback", "bgm", "/api/bgm/oauth/callback", "POST", false, false, "Submit Bangumi OAuth callback code"),

            ep("playList", "play", "/api/playList", "POST", false, false, "Get playlist entries"),
            ep("getSubtitles", "play", "/api/getSubtitles", "POST", false, false, "Read embedded subtitles"),
            ep("startCollection", "play", "/api/startCollection", "POST", false, false, "Start collection download"),
            ep("previewCollection", "play", "/api/previewCollection", "POST", false, false, "Preview collection file tree"),
            ep("getCollectionSubgroup", "play", "/api/getCollectionSubgroup", "POST", false, false, "Resolve collection subgroup"),

            ep("torrentsInfos", "download", "/api/torrentsInfos", "POST", false, false, "List torrents info"),
            ep("deleteTorrent", "download", "/api/deleteTorrent", "POST", false, false, "Delete cached torrent metadata"),

            ep("testNotification", "notification", "/api/testNotification", "POST", false, false, "Test a notification config"),
            ep("newNotification", "notification", "/api/newNotification", "POST", false, false, "Create default notification config"),
            ep("getTgUpdates", "notification", "/api/getTgUpdates", "POST", false, false, "Read Telegram updates"),

            ep("logs", "logs", "/api/logs", "POST", false, false, "Get runtime logs"),
            ep("clearLogs", "logs", "/api/clearLogs", "POST", false, false, "Clear runtime logs"),
            ep("downloadLogs", "logs", "/api/downloadLogs", "GET", false, true, "Download log archive"),

            ep("scrape", "metadata", "/api/scrape", "POST", false, false, "Trigger metadata scrape"),

            ep("getEmbyViews", "emby", "/api/getEmbyViews", "POST", false, false, "List Emby libraries"),
            ep("embyWebHook", "emby", "/api/embyWebHook", "POST", false, false, "Trigger Emby webhook handler"),

            ep("about", "system", "/api/about", "POST", false, false, "Get app information"),
            ep("update", "system", "/api/update", "POST", false, false, "Trigger update routine"),
            ep("stop", "system", "/api/stop", "POST", false, false, "Restart or stop service"),

            ep("file", "files", "/api/file", "GET", false, true, "Download local file"),
            ep("upload", "files", "/api/upload", "POST", true, false, "Upload one file"),

            ep("verifyNo", "afdian", "/api/verifyNo", "POST", false, false, "Verify Afdian order number"),
            ep("tryOut", "afdian", "/api/tryOut", "POST", false, false, "Request trial"),

            ep("login", "auth", "/api/login", "POST", false, false, "Login and obtain Authorization token"),
            ep("testIpWhitelist", "auth", "/api/testIpWhitelist", "POST", false, false, "Check IP whitelist login path")
    );

    private McpCatalog() {
    }

    private static McpEndpoint ep(
            String key,
            String group,
            String path,
            String method,
            boolean multipart,
            boolean binary,
            String description
    ) {
        return new McpEndpoint(key, group, path, method, multipart, binary, description);
    }

    public static List<McpEndpoint> endpoints() {
        return ENDPOINTS;
    }

    public static Optional<McpEndpoint> endpointByKey(String key) {
        if (Objects.isNull(key)) {
            return Optional.empty();
        }
        return ENDPOINTS.stream().filter(endpoint -> endpoint.key().equalsIgnoreCase(key)).findFirst();
    }

    public static Optional<McpEndpoint> endpointByToolName(String toolName) {
        if (Objects.isNull(toolName)) {
            return Optional.empty();
        }

        String normalized = toolName.trim();
        if (!normalized.startsWith("ani_rss.")) {
            return Optional.empty();
        }

        String key = normalized.substring("ani_rss.".length());
        return endpointByKey(key);
    }

    public static List<Map<String, Object>> tools() {
        Map<String, Object> genericProperties = new LinkedHashMap<>();
        genericProperties.put("endpointKey", Map.of("type", "string", "description", "Endpoint key from tools/list"));
        genericProperties.put("query", Map.of("type", "object", "additionalProperties", Map.of("type", "string")));
        genericProperties.put("body", Map.of("type", "object"));
        genericProperties.put("headers", Map.of("type", "object", "additionalProperties", Map.of("type", "string")));
        genericProperties.put("fileName", Map.of("type", "string"));
        genericProperties.put("fileBase64", Map.of("type", "string"));

        Map<String, Object> genericTool = new LinkedHashMap<>();
        genericTool.put("name", GENERIC_TOOL_NAME);
        genericTool.put("description", "Generic Ani-RSS API dispatcher");
        genericTool.put("inputSchema", Map.of(
                "type", "object",
                "properties", genericProperties,
                "required", List.of("endpointKey"),
                "additionalProperties", false
        ));

        List<Map<String, Object>> endpointTools = ENDPOINTS.stream().map(endpoint -> {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("query", Map.of("type", "object", "additionalProperties", Map.of("type", "string")));
            properties.put("body", Map.of("type", "object"));
            properties.put("headers", Map.of("type", "object", "additionalProperties", Map.of("type", "string")));
            if (endpoint.multipart()) {
                properties.put("fileName", Map.of("type", "string"));
                properties.put("fileBase64", Map.of("type", "string"));
            }

            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", endpoint.toolName());
            tool.put("description", endpoint.description());
            tool.put("inputSchema", Map.of(
                    "type", "object",
                    "properties", properties,
                    "additionalProperties", false
            ));
            return tool;
        }).toList();

        ArrayList<Map<String, Object>> tools = new ArrayList<>();
        tools.add(genericTool);
        tools.addAll(endpointTools);
        return tools;
    }
}
