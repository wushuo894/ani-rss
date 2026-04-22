package ani.rss.mcp;

public record McpEndpoint(
        String key,
        String group,
        String path,
        String method,
        boolean multipart,
        boolean binary,
        String description
) {
    public String toolName() {
        return "ani_rss." + key;
    }
}
