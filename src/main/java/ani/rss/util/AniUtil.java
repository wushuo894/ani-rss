package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AniUtil {
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Getter
    private static final List<Ani> aniList = new ArrayList<>();

    public static File getAniFile() {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        File configFile = new File("ani.json");
        if (StrUtil.isNotBlank(config)) {
            configFile = new File(config + File.separator + "ani.json");
        }
        return configFile;
    }

    public static void load() {
        File configFile = getAniFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(gson.toJson(aniList), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        JsonArray jsonElements = gson.fromJson(s, JsonArray.class);
        for (JsonElement jsonElement : jsonElements) {
            Ani ani = gson.fromJson(jsonElement, Ani.class);
            aniList.add(ani);
        }
    }

    public static void sync() {
        File configFile = getAniFile();
        String json = gson.toJson(aniList);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
    }

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

        int offset = ani.getOffset();
        int season = ani.getSeason();
        List<Item> items = new ArrayList<>();

        Document document = XmlUtil.readXML(xml);
        Node channel = document.getElementsByTagName("channel").item(0);
        NodeList childNodes = channel.getChildNodes();

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
            items.add(
                    new Item()
                            .setTitle(itemTitle)
                            .setTorrent(torrent)
                            .setLength(length)
            );
        }

        String s = "(.*|\\[.*])( -? \\d+|\\[\\d+]|\\[\\d+.?[vV]\\d]|第\\d+[话話集]|\\[第?\\d+[话話集]]|\\[\\d+.?END]|[Ee][Pp]?\\d+)(.*)";

        List<String> es = new ArrayList<>();
        items = items.parallelStream()
                .filter(item -> {
                    String itemTitle = item.getTitle();
                    String e = ReUtil.get(s, itemTitle, 2);
                    String episode = ReUtil.get("\\d+", e, 0);
                    if (StrUtil.isBlank(episode)) {
                        return false;
                    }
                    if (es.contains(episode)) {
                        return false;
                    }
                    es.add(episode);
                    item.setEpisode(Integer.parseInt(episode))
                            .setReName(
                                    StrFormatter.format("{} S{}E{}",
                                            title,
                                            String.format("%02d", season),
                                            String.format("%02d", Integer.parseInt(episode)))
                            );
                    return true;
                }).collect(Collectors.toList());

        return items;
    }

    public static List<Item> getItems(Ani ani) {
        String url = ani.getUrl();
        String s = HttpRequest.get(url)
                .thenFunction(HttpResponse::body);
        return getItems(ani, s);
    }

}
