package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AniUtil {

    public static Ani getAni(String url) {
        int season = 1;
        String title = "";

        String s = HttpRequest.get(url)
                .thenFunction(HttpResponse::body);
        Document document = XmlUtil.readXML(s);
        Node channel = document.getElementsByTagName("channel").item(0);
        NodeList childNodes = channel.getChildNodes();

        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node item = childNodes.item(i);
            String nodeName = item.getNodeName();
            if (nodeName.equals("title")) {
                title = ReUtil.replaceAll(item.getTextContent(), "^Mikan Project - ", "");
            }
        }

        String seasonReg = "第(.+)季";
        if (ReUtil.contains(seasonReg, title)) {
            season = Convert.chineseToNumber(ReUtil.get(seasonReg, title, 1));
            title = ReUtil.replaceAll(title, seasonReg, "").trim();
        }

        String bangumiId = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8)
                .get("bangumiId");


        String cover = HttpRequest.get("https://mikanime.tv/Home/Bangumi/" + bangumiId)
                .thenFunction(res -> {
                    org.jsoup.nodes.Document html = Jsoup.parse(res.body());
                    Elements elementsByClass = html.getElementsByClass("bangumi-poster");
                    Element element = elementsByClass.get(0);
                    String style = element.attr("style");
                    String image = style.replace("background-image: url('", "").replace("');", "");
                    return "https://mikanime.tv" + image;
                });

        Ani ani = new Ani();
        ani.setOffset(0)
                .setUrl(url.trim())
                .setSeason(season)
                .setTitle(title.trim())
                .setCover(cover)
                .setExclude(List.of("720"));

        List<Item> items = getItems(ani, s);
        if (items.isEmpty()) {
            return ani;
        }

        int offset = items.stream()
                .map(Item::getEpisode)
                .min(Comparator.comparingInt(i -> i))
                .get() - 1;
        return ani.setOffset(offset);
    }

    public static List<Item> getItems(Ani ani, String xml) {
        String title = ani.getTitle();
        List<String> exclude = ani.getExclude();

        int off = ani.getOffset();
        int season = ani.getSeason();
        List<Item> items = new ArrayList<>();

        Document document = XmlUtil.readXML(xml);
        Node channel = document.getElementsByTagName("channel").item(0);
        NodeList childNodes = channel.getChildNodes();

        int episode = 1 + off;
        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node item = childNodes.item(i);
            String nodeName = item.getNodeName();
            if (!nodeName.equals("item")) {
                continue;
            }
            String itemTitle = "";
            String torrent = "";
            int length = 0;

            NodeList itemChildNodes = item.getChildNodes();
            for (int j = 0; j < itemChildNodes.getLength(); j++) {
                Node itemChild = itemChildNodes.item(j);
                String itemChildNodeName = itemChild.getNodeName();
                if (itemChildNodeName.equals("title")) {
                    itemTitle = itemChild.getTextContent();
                }
                if (itemChildNodeName.equals("enclosure")) {
                    NamedNodeMap attributes = itemChild.getAttributes();
                    torrent = attributes.getNamedItem("url").getNodeValue();
                    length = Integer.parseInt(attributes.getNamedItem("length").getNodeValue());
                }
            }

            // 进行过滤
            String finalItemTitle = itemTitle;
            if (exclude.stream().anyMatch(finalItemTitle::contains)) {
                continue;
            }

            List<String> strings = new ArrayList<>();
            Consumer<String> consumer = (e) -> {
                strings.add(" " + e + " ");
                strings.add("第" + e + "集");
                strings.add("第" + e + "话");
                strings.add("第" + e + "話");
                strings.add("[" + e + "]");
            };
            consumer.accept(String.valueOf(episode));
            consumer.accept(String.format("%02d", episode));
            consumer.accept(String.format("%03d", episode));
            consumer.accept(Convert.numberToChinese(episode, true));
            consumer.accept(Convert.numberToChinese(episode, false));

            if (strings.stream()
                    .noneMatch(finalItemTitle::contains)) {
                episode++;
                continue;
            }
            items.add(
                    new Item()
                            .setTitle(itemTitle)
                            .setTorrent(torrent)
                            .setLength(length)
                            .setEpisode(episode)
            );
            episode++;
        }

        for (Item item : items) {
            item.setReName(StrFormatter.format("{} S{}E{}", title, String.format("%02d", season), String.format("%02d", item.getEpisode())));
        }

        return items;
    }

    public static List<Item> getItems(Ani ani) {
        String url = ani.getUrl();
        String s = HttpRequest.get(url)
                .thenFunction(HttpResponse::body);
        return getItems(ani, s);
    }

}
