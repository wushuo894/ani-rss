package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Result;
import ani.rss.util.MavenUtil;
import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Path("/about")
public class AboutAction implements Action {
    private final Log log = Log.get(AboutAction.class);

    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        res.setContentType("application/json; charset=utf-8");
        String version = MavenUtil.getVersion();

        About about = new About()
                .setVersion(version)
                .setUpdate(false)
                .setLatest("");
        try {
            HttpRequest.get("https://github.com/wushuo894/ani-rss/releases/latest")
                    .setFollowRedirects(true)
                    .timeout(3000)
                    .then(response -> {
                        String body = response.body();
                        Document document = Jsoup.parse(body);
                        Elements elements = document.getElementsByTag("h1");
                        for (Element element : elements) {
                            String trim = element.text().replace("v", "").trim();
                            if (trim.startsWith("v")) {
                                about.setUpdate(VersionComparator.INSTANCE.compare(trim, version) > 0)
                                        .setLatest(trim);
                            }
                        }
                    });
        } catch (Exception e) {
            log.error(e);
        }

        String json = gson.toJson(Result.success().setData(about));
        IoUtil.writeUtf8(res.getOut(), true, json);
    }

    @Data
    @Accessors(chain = true)
    public static class About {
        private String version;
        private String latest;
        private Boolean update;
    }
}
