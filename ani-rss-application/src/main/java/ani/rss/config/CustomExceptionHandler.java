package ani.rss.config;

import ani.rss.entity.Result;
import ani.rss.exception.ResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(ResultException.class)
    public Result<Void> handleException(ResultException e) {
        return e.getResult();
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public Result<Void> handleException(NoResourceFoundException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.error();
    }

}
