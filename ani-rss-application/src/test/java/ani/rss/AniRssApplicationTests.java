package ani.rss;

import ani.rss.util.other.TemplateUtil;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
class AniRssApplicationTests {

    @Test
    void mailTest() {
        Parser parser = Parser.builder().build();
        Node document = parser.parse("""
                # 测试
                ## 测试
                ～测试～
                --测试--
                **测试**
                """);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String render = renderer.render(document);

        Map<String, Object> map = Map.of(
                "render", render,
                "image", "https://lain.bgm.tv/pic/cover/l/99/17/292970_mxMxx.jpg",
                "mailImage", false
        );

        String html = TemplateUtil.render("mail.html", map);

        System.out.println(html);
    }

}
