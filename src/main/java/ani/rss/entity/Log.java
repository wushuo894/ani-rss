package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 日志
 */
@Data
@Accessors(chain = true)
public class Log implements Serializable {

    /**
     * 日志信息
     */
    private String message;

    /**
     * 日志级别
     */
    private String level;

    /**
     * 类路径
     */
    private String loggerName;

    /**
     * 线程名
     */
    private String threadName;
}
