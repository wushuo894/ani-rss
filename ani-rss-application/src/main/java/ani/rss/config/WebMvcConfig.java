package ani.rss.config;

import ani.rss.commons.GsonStatic;
import cn.hutool.core.util.ClassUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

import java.nio.charset.StandardCharsets;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        PathPatternParser pathPatternParser = new PathPatternParser();

        // 设置不区分大小写
        pathPatternParser.setCaseSensitive(false);

        configurer.setPatternParser(pathPatternParser)
                .addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConvertersList(converters -> {
            GsonHttpMessageConverter converter = new GsonHttpMessageConverter() {
                @Override
                public boolean canRead(@NonNull Class<?> clazz, @Nullable MediaType mediaType) {
                    if (ClassUtil.isSimpleTypeOrArray(clazz)) {
                        // 简单值类型或简单值类型的数组
                        return false;
                    }

                    if (isMcp(clazz)) {
                        // mcp 不使用 gson
                        return false;
                    }

                    return super.canRead(clazz, mediaType);
                }

                @Override
                public boolean canWrite(@NonNull Class<?> clazz, @Nullable MediaType mediaType) {
                    if (ClassUtil.isSimpleTypeOrArray(clazz)) {
                        // 简单值类型或简单值类型的数组
                        return false;
                    }

                    if (isMcp(clazz)) {
                        // mcp 不使用 gson
                        return false;
                    }
                    return super.canWrite(clazz, mediaType);
                }
            };
            converter.setGson(GsonStatic.GSON);
            converter.setDefaultCharset(StandardCharsets.UTF_8);
            converters.add(0, converter);
        });
    }

    /**
     * 判断是否为MCP的类
     *
     * @param clazz class
     * @return true/false
     */
    private boolean isMcp(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("io.modelcontextprotocol.");
    }
}
