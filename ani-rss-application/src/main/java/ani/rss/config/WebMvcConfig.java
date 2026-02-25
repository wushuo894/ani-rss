package ani.rss.config;

import ani.rss.commons.GsonStatic;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

    @Bean
    public Gson gson() {
        return GsonStatic.GSON;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConvertersList(converters -> {
            GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
            converter.setGson(gson());
            converter.setDefaultCharset(StandardCharsets.UTF_8);
            converters.add(0, converter);
        });
    }
}
