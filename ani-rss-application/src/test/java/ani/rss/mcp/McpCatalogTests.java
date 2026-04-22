package ani.rss.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpCatalogTests {

    @Test
    void shouldResolveEndpointByToolName() {
        McpEndpoint endpoint = McpCatalog.endpointByToolName("ani_rss.listAni")
                .orElseThrow();

        assertEquals("listAni", endpoint.key());
        assertEquals("/api/listAni", endpoint.path());
        assertEquals("POST", endpoint.method());
    }

    @Test
    void shouldExposeGenericAndEndpointTools() {
        List<Map<String, Object>> tools = McpCatalog.tools();

        assertTrue(tools.stream().anyMatch(tool -> McpCatalog.GENERIC_TOOL_NAME.equals(tool.get("name"))));
        assertTrue(tools.stream().anyMatch(tool -> "ani_rss.config".equals(tool.get("name"))));

        int expectedSize = McpCatalog.endpoints().size() + 1;
        assertEquals(expectedSize, tools.size());
    }

    @Test
    void shouldReturnEmptyForUnknownEndpoint() {
        assertTrue(McpCatalog.endpointByKey("unknown").isEmpty());
        assertTrue(McpCatalog.endpointByToolName("ani_rss.unknown").isEmpty());
    }
}
