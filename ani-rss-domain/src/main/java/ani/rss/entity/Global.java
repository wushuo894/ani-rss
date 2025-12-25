package ani.rss.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class Global implements Serializable {
    public static List<String> ARGS = new ArrayList<>();

    public static String HOST = "";
    public static String HTTP_PORT = "7789";
}
