package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class MikanUtil {
    private static final String host = "https://mikanime.tv";

    public static Mikan list(String text, Mikan.Season season) {
        String url = host;
        if (StrUtil.isNotBlank(text)) {
            url = url + "/Home/Search?searchstr=" + text;
        } else {
            Integer year = season.getYear();
            String seasonStr = season.getSeason();
            if (Objects.nonNull(year) && StrUtil.isNotBlank(seasonStr)) {
                url = url + "/Home/BangumiCoverFlowByDayOfWeek?year=" + year + "&seasonStr=" + seasonStr;
            }
        }
        return HttpReq.get(url)
                .thenFunction(res -> {
                    Mikan mikan = new Mikan();
                    List<Mikan.Item> items = new ArrayList<>();
                    List<Mikan.Season> seasons = new ArrayList<>();

                    Document document = Jsoup.parse(res.body());
                    Elements dateSelects = document.select(".date-select");
                    if (!dateSelects.isEmpty()) {
                        Element dateSelect = dateSelects.get(0);
                        String dateText = dateSelects.get(0).select(".date-text").text().trim();
                        Element dropdownMenu = dateSelect.selectFirst(".dropdown-menu");
                        for (Element child : dropdownMenu.children()) {
                            Elements seasonItems = child.select("li");
                            for (Element seasonItem : seasonItems.subList(1, seasonItems.size())) {
                                Element a = seasonItem.selectFirst("a");
                                String dataYear = a.attr("data-year");
                                String dataSeason = a.attr("data-season");
                                seasons.add(new Mikan.Season()
                                        .setYear(Integer.parseInt(dataYear))
                                        .setSeason(dataSeason)
                                        .setSelect(dateText.equals(dataYear + " " + a.text())));
                            }
                        }
                    }

                    Function<Element, List<Ani>> get = (el) -> {
                        List<Ani> anis = new ArrayList<>();
                        Elements lis = el.select("li");
                        for (Element li : lis) {
                            String img = host + li.selectFirst("span")
                                    .attr("data-src");
                            Elements aa = li.select("a");
                            if (aa.isEmpty()) {
                                continue;
                            }
                            String href = host + aa.get(0).attr("href");
                            String title = aa.get(0).text();
                            anis.add(new Ani()
                                    .setCover(img)
                                    .setTitle(title)
                                    .setUrl(href));
                        }
                        return anis;
                    };

                    Elements skBangumis = document.select(".sk-bangumi");

                    if (skBangumis.isEmpty()) {
                        List<Ani> anis = get.apply(document.selectFirst(".an-ul"));

                        Mikan.Item item = new Mikan.Item();
                        items.add(item);
                        item.setItems(anis)
                                .setLabel("Search");
                    } else {
                        for (Element skBangumi : skBangumis) {
                            Mikan.Item item = new Mikan.Item();
                            items.add(item);
                            String label = skBangumi.children().get(0).text().trim();
                            item.setLabel(label);
                            List<Ani> anis = get.apply(skBangumi);
                            item.setItems(anis);
                        }
                    }
                    return mikan
                            .setItems(items)
                            .setSeasons(seasons);
                });
    }

    public static List<Mikan.Group> getGroups(String url) {
        return HttpReq.get(url)
                .thenFunction(res -> {
                    Document document = Jsoup.parse(res.body());
                    List<Mikan.Group> groups = new ArrayList<>();

                    Elements subgroupTitles = document.select(".leftbar-item");

                    for (Element subgroupText : subgroupTitles) {
                        Mikan.Group group = new Mikan.Group();

                        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                        group.setItems(torrentsInfos);
                        String label = subgroupText.select("a.subgroup-name").text();
                        // id锚点，例如 #213
                        String id = subgroupText.select("a.subgroup-name").attr("data-anchor");
                        String attr = document.selectFirst(id).selectFirst(".mikan-rss").attr("href");
                        group.setLabel(label)
                                .setRss(host + attr);
                        groups.add(group);
                        // 字幕组更新日期
                        String day = subgroupText.select(".date").text();
                        group.setUpdateDay(day);

                        Element table = document.selectFirst(id).nextElementSibling();
                        Element tbody = table.selectFirst("tbody");
                        for (Element tr : tbody.children()) {
                            String s = tr.select("a").get(0).ownText();
                            String sizeStr = tr.select("td").get(1).text().trim();
                            String dataStr = tr.select("td").get(2).text().trim();
                            torrentsInfos.add(
                                    new TorrentsInfo()
                                            .setName(s)
                                            .setSizeStr(sizeStr)
                                            .setDateStr(dataStr)
                            );
                        }
                    }

                    return groups;
                });
    }

}
