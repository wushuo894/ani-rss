package ani.rss.mcp.dto;

import ani.rss.entity.Mikan;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class SearchMikanDTO implements Serializable {
    @McpToolParam(description = "搜索关键词, 默认传入空字符串", required = false)
    private String text;
    @McpToolParam(description = "可选季度过滤，例如年份和季度", required = false)
    private Mikan.Season season;
}
