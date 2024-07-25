package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
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
import java.util.*;
import java.util.stream.Collectors;

public class AniUtil {
    private static final Log LOG = Log.get(AniUtil.class);

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    @Getter
    private static final List<Ani> ANI_LIST = new Vector<>();

    /**
     * 获取订阅配置文件
     *
     * @return
     */
    public static File getAniFile() {
        File configDir = ConfigUtil.getConfigDir();
        return new File(configDir + File.separator + "ani.json");
    }

    /**
     * 加载订阅
     */
    public static void load() {
        File configFile = getAniFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(GSON.toJson(ANI_LIST), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        JsonArray jsonElements = GSON.fromJson(s, JsonArray.class);
        for (JsonElement jsonElement : jsonElements) {
            Ani ani = GSON.fromJson(jsonElement, Ani.class);
            ANI_LIST.add(ani);
        }
        LOG.debug("加载订阅 共{}项", ANI_LIST.size());
    }

    /**
     * 将订阅配置保存到磁盘
     */
    public static void sync() {
        File configFile = getAniFile();
        String json = GSON.toJson(ANI_LIST);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
        LOG.debug("保存订阅 {}", configFile);
    }

    /**
     * 获取动漫信息
     *
     * @param url
     * @return
     */
    public static Ani getAni(String url) {
        int season = 1;
        String title = "无";

        String s = HttpRequest.get(url)
                .setFollowRedirects(true)
                .thenFunction(HttpResponse::body);
        Document document = XmlUtil.readXML(s);
        Node channel = document.getElementsByTagName("channel").item(0);
        NodeList childNodes = channel.getChildNodes();

        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node item = childNodes.item(i);
            String nodeName = item.getNodeName();
            if (nodeName.equals("title")) {
                title = ReUtil.replaceAll(item.getTextContent(), "^Mikan Project - ", "").trim();
            }
        }

        String seasonReg = "第(.+)季";
        if (ReUtil.contains(seasonReg, title)) {
            season = Convert.chineseToNumber(ReUtil.get(seasonReg, title, 1));
            title = ReUtil.replaceAll(title, seasonReg, "").trim();
        }

        String bangumiId = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8)
                .get("bangumiId");


        String cover = HttpRequest.get(URLUtil.getHost(URLUtil.url(url)) + "/Home/Bangumi/" + bangumiId)
                .setFollowRedirects(true)
                .thenFunction(res -> {
                    org.jsoup.nodes.Document html = Jsoup.parse(res.body());
                    Elements elementsByClass = html.getElementsByClass("bangumi-poster");
                    Element element = elementsByClass.get(0);
                    String style = element.attr("style");
                    String image = style.replace("background-image: url('", "").replace("');", "");
                    HttpConnection httpConnection = (HttpConnection) ReflectUtil.getFieldValue(res, "httpConnection");
                    return URLUtil.getHost(httpConnection.getUrl()) + image;
                });

        File jpgFile = new File(URLUtil.toURI(cover).getPath());
        String dir = jpgFile.getParentFile().getName();
        String filename = jpgFile.getName();
        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/" + dir);
        File file = new File(configDir + "/files/" + dir + "/" + filename);
        HttpUtil.downloadFile(cover, file);

        Ani ani = new Ani();
        ani.setOffset(0)
                .setUrl(url.trim())
                .setSeason(season)
                .setTitle(title.trim())
                .setCover(dir + "/" + filename)
                .setExclude(List.of("720"));

        LOG.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GSON.toJson(ani)));

        List<Item> items = getItems(ani, s);
        LOG.debug("获取到视频 共{}个", items.size());
        if (items.isEmpty()) {
            return ani;
        }
        int offset = -(items.stream()
                .map(Item::getEpisode)
                .min(Comparator.comparingInt(i -> i))
                .get() - 1);
        LOG.debug("自动获取到剧集偏移为 {}", offset);
        return ani.setOffset(offset);
    }

    /**
     * 获取视频列表
     *
     * @param ani
     * @param xml
     * @return
     */
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
        items = items.stream()
                .filter(item -> {
                    try {
                        String itemTitle = item.getTitle();
                        String e = ReUtil.get(s, itemTitle, 2);
                        String episode = ReUtil.get("\\d+", e, 0);
                        if (StrUtil.isBlank(episode)) {
                            return false;
                        }
                        if (es.contains(episode)) {
                            return false;
                        }
                        item.setEpisode(Integer.parseInt(episode) + offset);
                        es.add(String.valueOf(item.getEpisode()));
                        item
                                .setReName(
                                        StrFormatter.format("{} S{}E{}",
                                                title,
                                                String.format("%02d", season),
                                                String.format("%02d", item.getEpisode()))
                                );
                        return true;
                    } catch (Exception e) {
                        LOG.error("解析rss视频集次出现问题");
                        LOG.error(e);
                    }
                    return false;
                }).collect(Collectors.toList());

        return items;
    }

    /**
     * 获取视频列表
     *
     * @param ani
     * @return
     */
    public static List<Item> getItems(Ani ani) {
        String url = ani.getUrl();
        String s = HttpRequest.get(url)
                .setFollowRedirects(true)
                .thenFunction(HttpResponse::body);
        return getItems(ani, s);
    }

    /**
     * 校验参数
     *
     * @param ani
     */
    public static void verify(Ani ani) {
        String url = ani.getUrl();
        List<String> exclude = ani.getExclude();
        Integer season = ani.getSeason();
        Integer offset = ani.getOffset();
        String title = ani.getTitle();
        Assert.notBlank(url, "RSS URL 不能为空");
        if (Objects.isNull(exclude)) {
            ani.setExclude(new ArrayList<>());
        }
        Assert.notNull(season, "季不能为空");
        Assert.notBlank(title, "标题不能为空");
        Assert.notNull(offset, "集数偏移不能为空");
    }

}
