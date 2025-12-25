package ani.rss.entity;

import cn.hutool.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

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
    private Integer code;
    private String message;
    private T data;
    private Long t;

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
}
