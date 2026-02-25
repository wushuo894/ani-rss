package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@Schema(description = "BGM 用户信息")
public class BgmMe implements Serializable {
    @Schema(description = "头像")
    private Avatar avatar;
    @Schema(description = "用户ID")
    private Integer id;
    @Schema(description = "签名")
    private String sign;
    @Schema(description = "主页地址")
    private String url;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @SerializedName(value = "userGroup", alternate = "user_group")
    @Schema(description = "用户组")
    private String userGroup;
    @SerializedName(value = "regTime", alternate = "reg_time")
    @Schema(description = "注册时间")
    private Date regTime;
    @Schema(description = "邮箱")
    private String email;
    @SerializedName(value = "timeOffset", alternate = "time_offset")
    @Schema(description = "时区偏移")
    private Integer timeOffset;
    @SerializedName(value = "expiresDays", alternate = "expires_days")
    @Schema(description = "过期天数")
    private Integer expiresDays;

    @Data
    @Accessors(chain = true)
    @Schema(description = "头像")
    public static class Avatar implements Serializable {
        @Schema(description = "large")
        private String large;
        @Schema(description = "medium")
        private String medium;
        @Schema(description = "small")
        private String small;
    }
}

