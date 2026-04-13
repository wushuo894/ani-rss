package ani.rss;

import ani.rss.entity.Global;
import ani.rss.util.other.SystemTrayUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;
import java.util.List;

@EnableScheduling
@SpringBootApplication
public class AniRssApplication {

    public static void main(String[] args) {
        Global.ARGS = List.of(ObjectUtil.defaultIfNull(args, new String[]{}));
        loadProperty();
        SystemTrayUtil.start();
        SpringApplication.run(AniRssApplication.class, args);
    }

    public static void loadProperty() {
        // 隐藏dock栏图标
        System.setProperty("apple.awt.UIElement", "true");
        // 菜单栏图标颜色自适应
        System.setProperty("apple.awt.enableTemplateImages", "true");
        // 启用Basic认证
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // DNS解析成功过期时间
        Security.setProperty("networkaddress.cache.ttl", "30");
        // DNS解析失败过期时间
        Security.setProperty("networkaddress.cache.negative.ttl", "5");
    }

}
