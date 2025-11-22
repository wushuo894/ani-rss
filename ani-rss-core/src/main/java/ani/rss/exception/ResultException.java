package ani.rss.exception;

import ani.rss.entity.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 返回结果异常
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResultException extends RuntimeException
        implements Serializable {
    public final Result<Void> result;
}
