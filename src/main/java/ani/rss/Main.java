package ani.rss;

import ani.rss.mcp.MCPService;
import ani.rss.other.Cron;
import ani.rss.util.*;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
public class Main {

    public static List<String> ARGS = new ArrayList<>();

    public static void main(String[] args) {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        Main.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        try {
            ConfigUtil.load();
            MenuUtil.start();
            ServerUtil.start();

            AniUtil.load();
            TaskUtil.start();
            String version = MavenUtil.getVersion();
            log.info("version {}", version);

            Cron.start();

            if (ARGS.contains("--mcp")) {
                SpringApplication.run(Main.class, args);
            }

        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
    }

    @Bean
    public List<ToolCallback> tools(MCPService service) {
        return List.of(ToolCallbacks.from(service));
    }

}
