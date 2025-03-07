package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 代理测试 相应体
 */
@Data
@Accessors(chain = true)
public class ProxyTest implements Serializable {
    /**
     * 状态码
     */
    private Integer status;

    /**
     * 耗时
     */
    private Long time;
}
