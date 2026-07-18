package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class GitInfo implements Serializable {
    private String branch;
    private String shortCommitId;
    private String commitId;
}
