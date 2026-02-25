package ani.rss.config;

import ani.rss.commons.MavenUtils;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI springShopOpenAPI() {
        Info info = new Info();

        License license = new License()
                .name("GPL-2.0")
                .url("https://github.com/wushuo894/ani-rss/blob/master/LICENSE");

        String version = MavenUtils.getVersion();

        info.title("ANI-RSS")
                .contact(new Contact())
                .description("基于RSS自动追番、订阅、下载、刮削")
                .version("v" + version)
                .license(license);

        ExternalDocumentation externalDocumentation = new ExternalDocumentation()
                .description("外部文档")
                .url("https://docs.wushuo.top/");


        return new OpenAPI()
                .info(info)
                .externalDocs(externalDocumentation);
    }
}
