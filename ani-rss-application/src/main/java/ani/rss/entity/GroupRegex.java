package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class GroupRegex implements Serializable {
    /**
     * Regex
     */
    @Schema(description = "正则表达式列表")
    private List<List<RegexItem>> regexList;

    @Schema(description = "标签集合")
    private List<String> tags;

    @Data
    @Accessors(chain = true)
    @Schema(description = "正则表达式项")
    public static class RegexItem implements Serializable {
        @Schema(description = "标签")
        private String label;
        @Schema(description = "正则表达式")
        private String regex;
    }
}
