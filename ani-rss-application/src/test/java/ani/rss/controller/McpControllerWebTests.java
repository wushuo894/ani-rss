package ani.rss.controller;

import ani.rss.mcp.McpApiProxyService;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpControllerWebTests {
    private MockMvc mockMvc;
    private McpApiProxyService mcpApiProxyService;

        @BeforeEach
        void setUp() {
                mcpApiProxyService = mock(McpApiProxyService.class);
                McpController controller = new McpController(mcpApiProxyService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

    @Test
    void initializeShouldReturnServerInfo() throws Exception {
        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {}
                }
                """;

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result.serverInfo.name").value("ani-rss-mcp"));
    }

    @Test
    void toolsListShouldExposeKnownTool() throws Exception {
        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list",
                  "params": {}
                }
                """;

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.tools[0].name").value("ani_rss.api_call"));
    }

    @Test
    void toolsCallShouldUseProxyService() throws Exception {
        JsonObject proxyResult = new JsonObject();
        proxyResult.addProperty("ok", true);
        proxyResult.addProperty("status", 200);

        when(mcpApiProxyService.callEndpoint(any(), any(), any())).thenReturn(proxyResult);

        String body = """
                {
                  "jsonrpc": "2.0",
                  "id": 3,
                  "method": "tools/call",
                  "params": {
                    "name": "ani_rss.listAni",
                    "arguments": {}
                  }
                }
                """;

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isError").value(false))
                                .andExpect(jsonPath("$.result.content[0].text", containsString("\"status\": 200")));
    }
}
