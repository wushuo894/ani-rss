package ani.rss;

import ani.rss.entity.Global;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;
import java.util.List;
import java.util.Map;

@EnableScheduling
@SpringBootApplication
public class AniRssApplication {

    public static void main(String[] args) {
        Global.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        loadProperty();
        SpringApplication.run(AniRssApplication.class, args);
    }

    public static void loadProperty() {
        // 启用Basic认证
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // DNS解析成功过期时间
        Security.setProperty("networkaddress.cache.ttl", "30");
        // DNS解析失败过期时间
        Security.setProperty("networkaddress.cache.negative.ttl", "5");

        // 处理命令行参数
        Map<String, String> mapping = Map.of(
                "--config", "CONFIG",
                "--mcp-enabled", "MCP_ENABLED",
                "--swagger-enabled", "SWAGGER_ENABLED"
        );

        mapping.forEach((k, v) -> {
            for (String arg : Global.ARGS) {
                String separator = k + "=";
                if (arg.startsWith(separator)) {
                    String value = StrUtil.subAfter(arg, separator, false);
                    System.setProperty(v, value);
                    return;
                }
            }
        });
    }

}
