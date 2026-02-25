package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 日志
 */
@Data
@Accessors(chain = true)
@Schema(description = "日志")
public class Log implements Serializable {

    /**
     * 日志信息
     */
    @Schema(description = "日志信息")
    private String message;

    /**
     * 日志级别
     */
    @Schema(description = "日志级别")
    private String level;

    /**
     * 类路径
     */
    @Schema(description = "类路径")
    private String loggerName;

    /**
     * 线程名
     */
    @Schema(description = "线程名")
    private String threadName;
}
