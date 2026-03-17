package ani.rss.config;

import ani.rss.entity.web.Result;
import ani.rss.exception.ResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> illegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(ResultException.class)
    public Result<Void> resultException(ResultException e) {
        return e.getResult();
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class, HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> notFoundException() {
        return new Result<>(404, "404 Not Found !");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.error();
    }

}
