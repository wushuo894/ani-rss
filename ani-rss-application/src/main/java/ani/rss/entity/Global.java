package ani.rss.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@Schema(description = "全局变量")
public class Global implements Serializable {
    public static List<String> ARGS = new ArrayList<>();

    public static final ThreadLocal<HttpServletRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServletResponse> RESPONSE = new ThreadLocal<>();
}
