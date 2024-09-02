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
    private static final String host = "https://mikanani.me";

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
                    Elements dateSelects = document.getElementsByClass("date-select");
                    if (!dateSelects.isEmpty()) {
                        Element dateSelect = dateSelects.get(0);
                        String dateText = dateSelects.get(0).getElementsByClass("date-text").text().trim();
                        Element dropdownMenu = dateSelect.getElementsByClass("dropdown-menu").get(0);
                        for (Element child : dropdownMenu.children()) {
                            Elements seasonItems = child.getElementsByTag("li");
                            for (Element seasonItem : seasonItems.subList(1, seasonItems.size())) {
                                Element a = seasonItem.getElementsByTag("a").get(0);
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
                        Elements lis = el.getElementsByTag("li");
                        for (Element li : lis) {
                            String img = host + li.getElementsByTag("span")
                                    .get(0).attr("data-src");
                            Elements aa = li.getElementsByTag("a");
                            if (aa.isEmpty()) {
                                continue;
                            }
                            String href = host + aa.get(0).attr("href");
                            String title = aa.get(0).attr("title");
                            anis.add(new Ani()
                                    .setCover(img)
                                    .setTitle(title)
                                    .setUrl(href));
                        }
                        return anis;
                    };

                    Elements skBangumis = document.getElementsByClass("sk-bangumi");

                    if (skBangumis.isEmpty()) {
                        List<Ani> anis = get.apply(document.getElementsByClass("an-ul").get(0));

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
                    Elements subgroupTexts = document.getElementsByClass("subgroup-text");
                    for (Element subgroupText : subgroupTexts) {
                        Element table = subgroupText.nextElementSibling();
                        Mikan.Group group = new Mikan.Group();

                        List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                        group.setItems(torrentsInfos);
                        String attr = subgroupText.getElementsByTag("a").get(1).attr("href");
                        String label = subgroupText.getElementsByTag("a").get(0).ownText();
                        label = StrUtil.blankToDefault(label, "生肉/不明字幕");
                        group.setLabel(label)
                                .setRss(host + attr);
                        groups.add(group);

                        Element tbody = table.getElementsByTag("tbody").get(0);
                        for (Element tr : tbody.children()) {
                            String s = tr.getElementsByTag("a").get(0).ownText();
                            String sizeStr = tr.getElementsByTag("td").get(1).text().trim();
                            String dataStr = tr.getElementsByTag("td").get(2).text().trim();
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
