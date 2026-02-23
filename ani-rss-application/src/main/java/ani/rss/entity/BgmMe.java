package ani.rss.entity;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
public class BgmMe implements Serializable {
    private Avatar avatar;
    private Integer id;
    private String sign;
    private String url;
    private String username;
    private String nickname;
    @SerializedName(value = "userGroup", alternate = "user_group")
    private String userGroup;
    @SerializedName(value = "regTime", alternate = "reg_time")
    private Date regTime;
    private String email;
    @SerializedName(value = "timeOffset", alternate = "time_offset")
    private Integer timeOffset;
    @SerializedName(value = "expiresDays", alternate = "expires_days")
    private Integer expiresDays;

    @Data
    @Accessors(chain = true)
    public static class Avatar implements Serializable {
        private String large;
        private String medium;
        private String small;
    }
}

