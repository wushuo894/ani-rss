package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 登录
 */
@Data
@Accessors(chain = true)
@Schema(description = "登录")
public class Login implements Serializable {
    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;
    /**
     * 密码
     */
    @Schema(description = "密码 (MD5摘要)")
    private String password;
    /**
     * ip
     */
    @Schema(description = "ip", hidden = true)
    private String ip;
    /**
     * key
     */
    @Schema(description = "key", hidden = true)
    private String key;
}
