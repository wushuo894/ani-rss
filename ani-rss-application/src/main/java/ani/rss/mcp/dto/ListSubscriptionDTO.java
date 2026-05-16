package ani.rss.mcp.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ListSubscriptionDTO implements Serializable {
    @McpToolParam(description = "可选的启用状态过滤：true 只返回启用订阅，false 只返回禁用订阅，不传则返回全部", required = false)
    private Boolean enabled;
}
