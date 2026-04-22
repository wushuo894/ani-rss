package ani.rss.controller;

import ani.rss.commons.GsonStatic;
import ani.rss.commons.MavenUtils;
import ani.rss.mcp.McpApiProxyService;
import ani.rss.mcp.McpCatalog;
import ani.rss.mcp.McpEndpoint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Hidden
public class McpController {
    private final McpApiProxyService mcpApiProxyService;

    @PostMapping(value = {"/mcp", "/mcp/"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String handle(@RequestBody String requestBody, HttpServletRequest request) {
        JsonElement id = null;
        try {
            JsonObject rpc = JsonParser.parseString(requestBody).getAsJsonObject();
            id = rpc.has("id") ? rpc.get("id") : null;
            String method = rpc.has("method") ? rpc.get("method").getAsString() : "";
            JsonObject params = getObject(rpc, "params");

            JsonObject result = switch (method) {
                case "initialize" -> initialize();
                case "ping", "notifications/initialized" -> new JsonObject();
                case "tools/list" -> toolsList();
                case "tools/call" -> toolsCall(params, request);
                default -> null;
            };

            if (result == null) {
                return error(id, -32601, "Method not found");
            }

            return success(id, result);
        } catch (IllegalArgumentException e) {
            return error(id, -32602, e.getMessage());
        } catch (Exception e) {
            return error(id, -32000, e.getMessage());
        }
    }

    private JsonObject initialize() {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");

        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "ani-rss-mcp");
        String version;
        try {
            version = MavenUtils.getVersion();
        } catch (Exception ignored) {
            version = "unknown";
        }
        serverInfo.addProperty("version", version);
        result.add("serverInfo", serverInfo);

        return result;
    }

    private JsonObject toolsList() {
        JsonObject result = new JsonObject();
        JsonArray tools = JsonParser.parseString(GsonStatic.toJson(McpCatalog.tools())).getAsJsonArray();
        result.add("tools", tools);
        return result;
    }

    private JsonObject toolsCall(JsonObject params, HttpServletRequest request) throws Exception {
        if (!params.has("name")) {
            throw new IllegalArgumentException("tools/call requires name");
        }

        String toolName = params.get("name").getAsString();
        JsonObject arguments = getObject(params, "arguments");

        McpEndpoint endpoint = resolveEndpoint(toolName, arguments);
        JsonObject proxyResult = mcpApiProxyService.callEndpoint(endpoint, arguments, request);

        JsonObject payload = new JsonObject();
        payload.addProperty("type", "text");
        payload.addProperty("text", GsonStatic.toJson(proxyResult));

        JsonArray content = new JsonArray();
        content.add(payload);

        JsonObject result = new JsonObject();
        result.add("content", content);
        result.addProperty("isError", proxyResult.has("ok") && !proxyResult.get("ok").getAsBoolean());

        return result;
    }

    private McpEndpoint resolveEndpoint(String toolName, JsonObject arguments) {
        if (McpCatalog.GENERIC_TOOL_NAME.equals(toolName)) {
            if (!arguments.has("endpointKey")) {
                throw new IllegalArgumentException("ani_rss.api_call requires endpointKey");
            }

            String endpointKey = arguments.get("endpointKey").getAsString();
            return McpCatalog.endpointByKey(endpointKey)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown endpointKey: " + endpointKey));
        }

        Optional<McpEndpoint> endpoint = McpCatalog.endpointByToolName(toolName);
        return endpoint.orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));
    }

    private static JsonObject getObject(JsonObject root, String key) {
        if (root == null || !root.has(key)) {
            return new JsonObject();
        }

        JsonElement element = root.get(key);
        if (!element.isJsonObject()) {
            return new JsonObject();
        }

        return element.getAsJsonObject();
    }

    private static String success(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? nullId() : id);
        response.add("result", result);
        return GsonStatic.toJson(response);
    }

    private static String error(JsonElement id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message == null ? "error" : message);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? nullId() : id);
        response.add("error", error);

        return GsonStatic.toJson(response);
    }

    private static JsonElement nullId() {
        return JsonParser.parseString("null");
    }
}
