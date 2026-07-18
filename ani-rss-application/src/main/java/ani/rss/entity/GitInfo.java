package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@Schema(description = "Git信息")
public class GitInfo implements Serializable {
    @Schema(description = "分支名称")
    private String branch;
    @Schema(description = "短提交ID")
    private String shortCommitId;
    @Schema(description = "提交ID")
    private String commitId;
}
