package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(description = "OpenList 离线任务信息")
public class OpenListTaskInfo implements Serializable {
    private String id;

    @Schema(description = "名称")
    private String name;

    private String creator;

    @SerializedName(value = "creatorRole", alternate = "creator_role")
    private Integer creatorRole;

    private State state;

    private String status;

    @Schema(description = "进度")
    private Integer progress;

    @Schema(description = "开始时间")
    @SerializedName(value = "startTime", alternate = "start_time")
    private Date startTime;

    @Schema(description = "结束时间")
    @SerializedName(value = "endTime", alternate = "end_time")
    private Date endTime;

    @Schema(description = "字节数")
    @SerializedName(value = "totalBytes", alternate = "total_bytes")
    private String totalBytes;

    @Schema(description = "错误信息")
    private String error;

    @AllArgsConstructor
    public static enum State implements IntEnum {
        // https://github.com/OpenListTeam/OpenList-Frontend/blob/d94691c110bb046465e526323f46ead8ddd83c20/src/lang/en/tasks.json#L14-L25
        Pending(0),
        Running(1),
        Succeeded(2),
        Canceling(3),
        Canceled(4),
        Error(5),
        Failing(6),
        Failed(7),
        Waiting_for_Retry(8),
        Preparing_to_Retry(9);

        @Getter
        private final int code;
    }

}
