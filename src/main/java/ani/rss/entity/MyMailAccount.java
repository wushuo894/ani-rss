package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 邮箱通知的配置
 * <p>
 * 具体注释见
 *
 * @see cn.hutool.extra.mail.MailAccount
 */
@Data
@Accessors(chain = true)
public class MyMailAccount implements Serializable {
    private String host;
    private Integer port;
    private String from;
    private String pass;
    private Boolean sslEnable;
}
