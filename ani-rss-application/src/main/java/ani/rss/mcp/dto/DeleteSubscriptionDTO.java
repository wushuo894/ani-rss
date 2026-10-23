package ani.rss.mcp.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.io.Serializable;
import java.util.List;

/**
 * MCP 删除订阅参数。
 */
@Data
@Accessors(chain = true)
public class DeleteSubscriptionDTO implements Serializable {

    @McpToolParam(description = "要删除的订阅 ID 列表")
    private List<String> ids;

    @McpToolParam(description = "是否同时删除下载器任务和本地视频文件，默认 false", required = false)
    private Boolean deleteFiles;
}
