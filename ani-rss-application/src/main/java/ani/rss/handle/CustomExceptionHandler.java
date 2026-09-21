package ani.rss.handle;

import ani.rss.entity.web.Result;
import ani.rss.exception.ResultException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    /**
     * 处理参数或状态不符合业务要求的异常。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 错误响应
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public Result<Void> argumentException(Exception e, HttpServletRequest request) {
        log.warn("请求参数或状态异常 {}", requestContext(request, e));
        return Result.error(errorMessage(e));
    }

    /**
     * 处理缺少必要请求参数的异常。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> missingParameterException(Exception e, HttpServletRequest request) {
        log.warn("请求缺少必要参数 {}", requestContext(request, e));
        return Result.error(errorMessage(e));
    }

    /**
     * 处理主动抛出的业务结果异常。
     *
     * @param e       业务结果异常
     * @param request 当前请求
     * @return 异常中携带的响应
     */
    @ExceptionHandler(ResultException.class)
    public Result<Void> resultException(ResultException e, HttpServletRequest request) {
        Result<Void> result = e.getResult();
        log.warn("业务请求失败 code={} message={} method={} path={} remote={}",
                result.getCode(),
                sanitize(result.getMessage()),
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
        return e.getResult();
    }

    /**
     * 处理未找到控制器或静态资源的请求。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 404 响应
     */
    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class
    })
    public Result<Void> notFoundException(Exception e, HttpServletRequest request) {
        // 未知资源请求较常见，仅在调试级别保留定位信息。
        log.debug("请求资源不存在 {}", requestContext(request, e));
        return new Result<>(404, "404 Not Found !");
    }

    /**
     * 处理请求方法不受支持的异常。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 404 响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> methodNotSupportedException(Exception e, HttpServletRequest request) {
        log.warn("请求方法不受支持 {}", requestContext(request, e));
        return new Result<>(404, "404 Not Found !");
    }

    /**
     * 处理未被其他处理器识别的异常。
     *
     * @param e       异常
     * @param request 当前请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 未知异常保留请求上下文与完整堆栈，便于定位服务端错误。
        log.error("请求处理异常 {}", requestContext(request, e), e);
        return Result.error(errorMessage(e));
    }

    /**
     * 生成不包含查询参数和请求体的安全请求上下文。
     *
     * @param request 当前请求
     * @param e       异常
     * @return 单行日志上下文
     */
    private String requestContext(HttpServletRequest request, Exception e) {
        return String.format(
                "method=%s path=%s remote=%s exception=%s message=%s",
                request.getMethod(),
                sanitize(request.getRequestURI()),
                sanitize(request.getRemoteAddr()),
                e.getClass().getSimpleName(),
                errorMessage(e)
        );
    }

    /**
     * 获取适合返回和记录的异常消息。
     *
     * @param e 异常
     * @return 非空的单行异常消息
     */
    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return sanitize(message);
    }

    /**
     * 清理日志字段中的换行符，避免破坏单条日志结构。
     *
     * @param value 原始字段
     * @return 可安全写入单行日志的字段
     */
    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

}
