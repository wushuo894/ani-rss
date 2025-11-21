package ani.rss.util.other;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Mikan;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.basic.GsonStatic;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class MikanUtil {
    public static String getMikanHost() {
        Config config = ConfigUtil.CONFIG;
        String mikanHost = config.getMikanHost();
        mikanHost = StrUtil.blankToDefault(mikanHost, "https://mikanime.tv");
        return mikanHost;
    }

    /**
     * 搜索mikan番剧列表
     *
     * @param text
     * @param season
     * @return
     */
    public static Mikan list(String text, Mikan.Season season) {
        Set<String> bangumiIdSet = AniUtil.ANI_LIST.stream()
                .map(AniUtil::getBangumiId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        String url = getMikanHost();
        if (StrUtil.isNotBlank(text)) {
            url = url + "/Home/Search?searchstr=" + URLUtil.encodeBlank(text);
        } else {
            Integer year = season.getYear();
            String seasonStr = season.getSeason();
            if (Objects.nonNull(year) && StrUtil.isNotBlank(seasonStr)) {
                url = url + "/Home/BangumiCoverFlowByDayOfWeek?year=" + year + "&seasonStr=" + seasonStr;
            }
        }

        AtomicReference<JsonObject> scoreAtomicReference = new AtomicReference<>();
        AtomicReference<Document> documentAtomicReference = new AtomicReference<>();
        String finalUrl = url;

        // 并行获取mikan番剧列表及其评分
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    JsonObject score = getScore();
                    scoreAtomicReference.set(score);
                }),
                CompletableFuture.runAsync(() -> {
                    Document document = HttpReq.get(finalUrl)
                            .thenFunction(res -> Jsoup.parse(res.body()));
                    documentAtomicReference.set(document);
                })
        ).join();

        Document document = documentAtomicReference.get();
        JsonObject score = scoreAtomicReference.get();

        Mikan mikan = new Mikan();
        List<Mikan.Item> items = new ArrayList<>();
        List<Mikan.Season> seasons = new ArrayList<>();

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
            if (Objects.isNull(el)) {
                return anis;
            }
            Elements lis = el.select("li");
            for (Element li : lis) {
                String img = getMikanHost() + li.selectFirst("span")
                        .attr("data-src");
                Elements aa = li.select("a");
                if (aa.isEmpty()) {
                    continue;
                }
                String href = getMikanHost() + aa.get(0).attr("href");
                String title = aa.get(0).text();

                String id = ReUtil.get("\\d+(/)?$", href, 0);
                id = StrUtil.blankToDefault(id, "");
                anis.add(new Ani()
                        .setCover(img)
                        .setTitle(title)
                        .setUrl(href)
                        .setExists(bangumiIdSet.contains(id))
                        .setScore(
                                Opt.ofNullable(score.get(id))
                                        .map(JsonElement::getAsDouble)
                                        .orElse(0.0)
                        )
                );
            }
            return ListUtil.sort(anis, Comparator.comparingDouble(Ani::getScore).reversed());
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
    }

    /**
     * 获取番剧字幕组
     *
     * @param url
     * @return
     */
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
                        String label = subgroupText.select("a.subgroup-name").text().trim();
                        // id锚点，例如 #213
                        String id = subgroupText.select("a.subgroup-name").attr("data-anchor");
                        String attr = document.selectFirst(id).selectFirst(".mikan-rss").attr("href");
                        group.setLabel(label)
                                .setRss(getMikanHost() + attr);
                        groups.add(group);
                        // 字幕组更新日期
                        String day = subgroupText.select(".date").text().trim();
                        group.setUpdateDay(day);

                        Element table = document.selectFirst(id).nextElementSibling();
                        Element tbody = table.selectFirst("tbody");
                        for (Element tr : tbody.children()) {
                            String s = tr.select("a").get(0).ownText();
                            String magnet = tr.select("a").get(1).attr("data-clipboard-text");
                            String sizeStr = tr.select("td").get(2).text().trim();
                            String dateStr = tr.select("td").get(3).text().trim();

                            String torrent = tr.select("a").get(2).attr("href");

                            String mikanHost = getMikanHost();

                            torrentsInfos.add(
                                    new TorrentsInfo()
                                            .setName(s)
                                            .setMagnet(magnet)
                                            .setSizeStr(sizeStr)
                                            .setDateStr(dateStr)
                                            .setTorrent(mikanHost + torrent)
                            );
                        }
                    }

                    return groups;
                });
    }

    public static void getMikanInfo(Ani ani, String subgroupId) {
        String bangumiId = AniUtil.getBangumiId(ani);
        if (StrUtil.isBlank(bangumiId)) {
            return;
        }
        HttpReq.get(URLUtil.getHost(URLUtil.url(getMikanHost())) + "/Home/Bangumi/" + bangumiId)
                .then(res -> {
                    org.jsoup.nodes.Document html = Jsoup.parse(res.body());

                    Element bangumiTitle = html.selectFirst(".bangumi-title");
                    ani.setMikanTitle(bangumiTitle.text().trim());

                    Elements bangumiInfos = html.select(".bangumi-info");
                    for (Element bangumiInfo : bangumiInfos) {
                        String string = bangumiInfo.ownText();
                        if (string.equals("Bangumi番组计划链接：")) {
                            String bgmUrl = bangumiInfo.selectFirst("a")
                                    .attr("href");
                            ani.setBgmUrl(bgmUrl);
                        }
                    }

                    if (StrUtil.isBlank(subgroupId)) {
                        return;
                    }

                    // 获取字幕组
                    Elements subgroupTexts = html.select(".subgroup-text");
                    for (Element subgroupText : subgroupTexts) {
                        String id = subgroupText.attr("id");
                        if (!id.equalsIgnoreCase(subgroupId)) {
                            continue;
                        }
                        String ownText = subgroupText.ownText().trim();
                        if (StrUtil.isNotBlank(ownText)) {
                            ani.setSubgroup(ownText.replace("/", "或"));
                            continue;
                        }
                        ani.setSubgroup(subgroupText.selectFirst("a").text().trim());
                    }
                });
    }

    /**
     * 从rss中获得字幕组id
     *
     * @param url
     * @return
     */
    public static String getSubgroupId(String url) {
        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);

        for (String k : decodeParamMap.keySet()) {
            String v = decodeParamMap.get(k);
            if (k.equalsIgnoreCase("subgroupid")) {
                return v;
            }
        }
        return "";
    }

    public static JsonObject getScore() {
        if (!AfdianUtil.verifyExpirationTime()) {
            return new JsonObject();
        }
        Config config = ConfigUtil.CONFIG;
        String outTradeNo = config.getOutTradeNo();
        boolean tryOut = config.getTryOut();
        if (StrUtil.isBlank(outTradeNo) || tryOut) {
            return new JsonObject();
        }

        JsonObject jsonObject = new JsonObject();
        try {
            String url = StrFormatter.format(
                    "https://bgm-cache.wushuo.top/score/{}/score.json",
                    SecureUtil.sha256(outTradeNo)
            );
            jsonObject = HttpReq.get(url)
                    .timeout(1000 * 5)
                    .thenFunction(res -> {
                        int status = res.getStatus();
                        if (status == 404) {
                            return new JsonObject();
                        }
                        HttpReq.assertStatus(res);
                        return GsonStatic.fromJson(res.body(), JsonObject.class);
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return jsonObject;
    }

}
