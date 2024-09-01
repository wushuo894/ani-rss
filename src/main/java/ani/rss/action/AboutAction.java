package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.util.HttpReq;
import ani.rss.util.MavenUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.Data;
import lombok.experimental.Accessors;
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
                        Element box = document.getElementsByClass("Box").get(0);
                        Element element = box.getElementsByTag("h1").get(0);
                        String latest = element.text().replace("v", "").trim();
                        about.setUpdate(VersionComparator.INSTANCE.compare(latest, version) > 0)
                                .setLatest(latest);
                        Element markdownBody = box.getElementsByClass("markdown-body").get(0);
                        about.setMarkdownBody(markdownBody.html());
                    });
        } catch (Exception e) {
            log.error(e.getMessage());
            log.debug(e.getMessage(), e);
        }

        resultSuccess(about);
    }

    @Data
    @Accessors(chain = true)
    public static class About {
        private String version;
        private String latest;
        private Boolean update;
        private String markdownBody;
    }
}
