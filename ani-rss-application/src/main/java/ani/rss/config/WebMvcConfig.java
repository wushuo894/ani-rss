package ani.rss.config;

import ani.rss.commons.GsonStatic;
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

import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
                    return clazz != String.class && !isMcpClass(clazz) && super.canRead(clazz, mediaType);
                }

                @Override
                public boolean canRead(@Nullable Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
                    return type != String.class
                            && !isMcpType(type)
                            && !isMcpClass(contextClass)
                            && super.canRead(type, contextClass, mediaType);
                }

                @Override
                public boolean canWrite(@NonNull Class<?> clazz, @Nullable MediaType mediaType) {
                    return !isMcpClass(clazz) && super.canWrite(clazz, mediaType);
                }

                @Override
                public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
                    return !isMcpType(type) && !isMcpClass(clazz) && super.canWrite(type, clazz, mediaType);
                }

                @Override
                protected void writeInternal(@NonNull Object object, @Nullable Type type, @NonNull Writer writer) throws Exception {
                    if (object instanceof byte[]) {
                        try (writer) {
                            writer.write(new String((byte[]) object, StandardCharsets.UTF_8));
                            writer.flush();
                        }
                        return;
                    }
                    super.writeInternal(object, type, writer);
                }

                private boolean isMcpType(@Nullable Type type) {
                    if (type instanceof Class<?> clazz) {
                        return isMcpClass(clazz);
                    }
                    if (type instanceof ParameterizedType parameterizedType) {
                        if (isMcpType(parameterizedType.getRawType())) {
                            return true;
                        }
                        for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
                            if (isMcpType(actualTypeArgument)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                private boolean isMcpClass(@Nullable Class<?> clazz) {
                    return clazz != null && clazz.getName().startsWith("io.modelcontextprotocol.");
                }
            };
            converter.setGson(GsonStatic.GSON);
            converter.setDefaultCharset(StandardCharsets.UTF_8);
            converters.add(0, converter);
        });
    }
}
