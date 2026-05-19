package ani.rss.exception;

import ani.rss.entity.web.Result;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
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
    private final Result<Void> result;

    public static ResultException exception(String message) {
        Result<Void> r = Result.error();
        r.setMessage(message);
        return new ResultException(r);
    }

    public ServerTransportSecurityException toServerTransportSecurityException() {
        Integer code = result.getCode();
        String message = result.getMessage();
        return new ServerTransportSecurityException(
                code,
                message
        );
    }
}
