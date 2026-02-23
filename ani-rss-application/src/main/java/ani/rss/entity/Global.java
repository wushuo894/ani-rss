package ani.rss.entity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class Global implements Serializable {
    public static List<String> ARGS = new ArrayList<>();

    public static final ThreadLocal<HttpServletRequest> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<HttpServletResponse> RESPONSE = new ThreadLocal<>();
}
