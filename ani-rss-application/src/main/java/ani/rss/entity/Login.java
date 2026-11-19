package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 登录
 */
@Data
@Accessors(chain = true)
@Schema(description = "登录")
@NoArgsConstructor
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

    public Login(String username, String password) {
        this.username = username;
        this.password = password;
        this.isDefault = false;
    }

    private Boolean isDefault;
}
