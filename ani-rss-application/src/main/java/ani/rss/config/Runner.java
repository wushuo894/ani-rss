package ani.rss.config;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.MavenUtils;
import ani.rss.service.TaskService;
import ani.rss.util.other.AniUtil;
import ani.rss.util.other.ConfigUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class Runner implements ApplicationRunner {

    @Value("${server.port}")
    private String port;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        try {
            ConfigUtil.load();
            ConfigUtil.backup();

            AniUtil.load();
            TaskService.start();
            String version = MavenUtils.getVersion();
            log.info("version {}", version);


            for (String ip : NetUtil.localIpv4s()) {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, Integer.parseInt(port));
                if (NetUtil.isOpen(inetSocketAddress, 100)) {
                    log.info("http://{}:{}", ip, port);
                }
            }
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            System.exit(1);
        }
        RuntimeUtil.addShutdownHook(() -> log.info("程序退出..."));
    }
}
