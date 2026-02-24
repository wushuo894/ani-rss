package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 代理测试 相应体
 */
@Data
@Accessors(chain = true)
@Schema(description = "代理测试响应体")
public class ProxyTest implements Serializable {
    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private Integer status;

    /**
     * 耗时
     */
    @Schema(description = "耗时")
    private Long time;
}
