package ani.rss.mcp.vo;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionItemsPreviewVO implements Serializable {
    @McpToolParam(description = "订阅信息")
    private Ani subscription;
    @McpToolParam(description = "预览资源信息")
    private List<Item> items;
}
