package ani.rss.mcp;

import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 快速启动MCPO接入OpenWebUI
 * <code>
 *     docker run -p 8050:8000 ghcr.io/open-webui/mcpo:main --server-type "sse" -- http://${your_id}:8080/sse
 * </code>
 * @see <a href="https://github.com/open-webui/mcpo">MCPO</a>
 * @see <a href="https://github.com/open-webui/open-webui">OpenWebUI</a>
 */
@Component
public class MCPService {

    @Tool(description = "获取当前(我)的订阅", name = "GetAniSubscribe")
    public List<Ani> getSubscribe() {
        return AniUtil.ANI_LIST;
    }
}
