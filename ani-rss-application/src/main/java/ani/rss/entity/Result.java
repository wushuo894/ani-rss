package ani.rss.entity;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * 返回包装体
 *
 * @param <T>
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> implements Serializable {
    @Schema(description = "状态码", defaultValue = "200")
    private Integer code;
    @Schema(description = "响应消息")
    private String message;
    @Schema(description = "数据", defaultValue = "null")
    private T data;
    @Schema(description = "时间戳", defaultValue = "current time")
    private Long t = System.currentTimeMillis();

    public static <Void> Result<Void> success() {
        return new Result<Void>()
                .setCode(HttpStatus.HTTP_OK)
                .setMessage("success");
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_OK)
                .setMessage("success")
                .setData(data);
    }

    public static <T> Result<T> success(String message) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_OK)
                .setMessage(message);
    }

    public static <T> Result<T> success(String message, Object... argArray) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_OK)
                .setMessage(StrFormatter.format(message, argArray));
    }

    public static <T> Result<T> success(Consumer<Result<T>> consumer) {
        Result<T> success = success();
        consumer.accept(success);
        return success;
    }

    public static <Void> Result<Void> error() {
        return new Result<Void>()
                .setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                .setMessage("error");
    }

    public static <T> Result<T> error(T data) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                .setMessage("error")
                .setData(data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                .setMessage(message);
    }

    public static <T> Result<T> error(String message, Object... argArray) {
        return new Result<T>()
                .setCode(HttpStatus.HTTP_INTERNAL_ERROR)
                .setMessage(StrFormatter.format(message, argArray));
    }

    public static <T> Result<T> error(Consumer<Result<T>> consumer) {
        Result<T> error = error();
        consumer.accept(error);
        return error;
    }
}
