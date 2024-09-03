package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.About;
import ani.rss.util.HttpReq;
import ani.rss.util.MavenUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@Slf4j
@Path("/about")
public class AboutAction implements BaseAction {

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        String version = MavenUtil.getVersion();

        About about = new About()
                .setVersion(version)
                .setUpdate(false)
                .setLatest("")
                .setMarkdownBody("");
        try {
            HttpReq.get("https://github.com/wushuo894/ani-rss/releases/latest")
                    .timeout(3000)
                    .then(response -> {
                        String body = response.body();
                        Document document = Jsoup.parse(body);
                        Element box = document.selectFirst(".Box");
                        Element element = box.selectFirst("h1");
                        String latest = element.text().replace("v", "").trim();
                        about.setUpdate(VersionComparator.INSTANCE.compare(latest, version) > 0)
                                .setLatest(latest);
                        Element markdownBody = box.selectFirst(".markdown-body");
                        about.setMarkdownBody(markdownBody.html());
                    });
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug(e.getMessage(), e);
        }

        resultSuccess(about);
    }


}
