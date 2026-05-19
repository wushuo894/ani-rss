package ani.rss.mcp;

import ani.rss.annotation.Auth;
import ani.rss.auth.enums.AuthType;
import ani.rss.exception.ResultException;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnClass(McpSchema.class)
@EnableConfigurationProperties(McpServerStreamableHttpProperties.class)
@Conditional({
        McpServerStdioDisabledCondition.class,
        McpServerAutoConfiguration.EnabledStreamableServerCondition.class
})
public class McpEndpointConfig implements WebMvcConfigurer {

    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties,
            McpTransportAuth mcpTransportAuth
    ) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .mcpEndpoint(serverProperties.getMcpEndpoint())
                .keepAliveInterval(serverProperties.getKeepAliveInterval())
                .disallowDelete(serverProperties.isDisallowDelete())
                .securityValidator(headers -> {
                    try {
                        mcpTransportAuth.validate();
                    } catch (ResultException e) {
                        throw e.toServerTransportSecurityException();
                    }
                })
                .build();
    }

    @Bean
    public McpTransportAuth mcpTransportAuth() {
        return new McpTransportAuth();
    }

    public static class McpTransportAuth {
        @Auth(type = AuthType.API_KEY)
        public void validate() {
        }
    }
}
