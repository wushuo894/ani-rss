package ani.rss.util.other;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.engine.thymeleaf.ThymeleafEngine;
import lombok.Synchronized;

import java.util.Map;

public class TemplateUtil {
    private static final TemplateEngine TEMPLATE_ENGINE;

    static {
        TemplateConfig templateConfig = new TemplateConfig("templates", TemplateConfig.ResourceMode.CLASSPATH);
        TEMPLATE_ENGINE = new ThymeleafEngine(templateConfig);
    }

    @Synchronized("TEMPLATE_ENGINE")
    public static String render(String resource, Map<?, ?> map) {
        Template template = TEMPLATE_ENGINE.getTemplate(resource);
        return template.render(map);
    }
}
