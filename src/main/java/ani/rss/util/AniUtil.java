package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class AniUtil {

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
            FileUtil.writeUtf8String(JSONUtil.formatJsonStr(GSON.toJson(ANI_LIST)), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        JsonArray jsonElements = GSON.fromJson(s, JsonArray.class);
        for (JsonElement jsonElement : jsonElements) {
            Ani ani = GSON.fromJson(jsonElement, Ani.class);
            Ani newAni = new Ani();
            newAni.setEnable(true);
            BeanUtil.copyProperties(ani, newAni, CopyOptions
                    .create()
                    .setIgnoreNullValue(true));
            ANI_LIST.add(newAni);
        }
        log.debug("加载订阅 共{}项", ANI_LIST.size());


        // 处理旧数据
        for (Ani ani : ANI_LIST) {
            String subgroup = StrUtil.blankToDefault(ani.getSubgroup(), "");
            ani.setSubgroup(subgroup);
            try {
                String cover = ani.getCover();
                if (!ReUtil.contains("http(s*)://", cover)) {
                    continue;
                }
                cover = AniUtil.saveJpg(cover);
                ani.setCover(cover);
                AniUtil.sync();
            } catch (Exception e) {
                log.error(e.getMessage());
                log.debug(e.getMessage(), e);
            }
        }
    }

    /**
     * 将订阅配置保存到磁盘
     */
    public static void sync() {
        File configFile = getAniFile();
        String json = GSON.toJson(ANI_LIST);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
        log.debug("保存订阅 {}", configFile);
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

        String s = HttpReq.get(url)
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

        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);

        String bangumiId = "", subgroupid = "";
        for (String k : decodeParamMap.keySet()) {
            String v = decodeParamMap.get(k);
            if (k.equalsIgnoreCase("bangumiId")) {
                bangumiId = v;
            }
            if (k.equalsIgnoreCase("subgroupid")) {
                subgroupid = v;
            }
        }

        Ani ani = new Ani();

        String finalSubgroupid = subgroupid;
        HttpReq.get(URLUtil.getHost(URLUtil.url(url)) + "/Home/Bangumi/" + bangumiId)
                .then(res -> {
                    org.jsoup.nodes.Document html = Jsoup.parse(res.body());

                    // 获取封面
                    Elements elementsByClass = html.getElementsByClass("bangumi-poster");
                    Element element = elementsByClass.get(0);
                    String style = element.attr("style");
                    String image = style.replace("background-image: url('", "").replace("');", "");
                    HttpConnection httpConnection = (HttpConnection) ReflectUtil.getFieldValue(res, "httpConnection");
                    String saveJpg = saveJpg(URLUtil.getHost(httpConnection.getUrl()) + image);
                    ani.setCover(saveJpg);

                    // 获取字幕组
                    Elements subgroupTexts = html.getElementsByClass("subgroup-text");
                    for (Element subgroupText : subgroupTexts) {
                        String id = subgroupText.attr("id");
                        if (!id.equalsIgnoreCase(finalSubgroupid)) {
                            continue;
                        }
                        String ownText = subgroupText.ownText().trim();
                        if (StrUtil.isNotBlank(ownText)) {
                            ani.setSubgroup(ownText);
                            continue;
                        }
                        ani.setSubgroup(subgroupText.getElementsByTag("a").get(0).text().trim());
                    }
                });

        ani.setOffset(0)
                .setUrl(url.trim())
                .setSeason(season)
                .setTitle(title.trim())
                .setEnable(true)
                .setExclude(List.of("720"));

        log.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GSON.toJson(ani)));

        List<Item> items = getItems(ani, s);
        log.debug("获取到视频 共{}个", items.size());
        if (items.isEmpty()) {
            return ani;
        }
        Config config = ConfigUtil.getCONFIG();
        // 自动推断剧集偏移
        if (config.getOffset()) {
            int offset = -(items.stream()
                    .map(Item::getEpisode)
                    .min(Comparator.comparingInt(i -> i))
                    .get() - 1);
            log.debug("自动获取到剧集偏移为 {}", offset);
            ani.setOffset(offset);
        }
        return ani;
    }

    public static String saveJpg(String coverUrl) {
        File jpgFile = new File(URLUtil.toURI(coverUrl).getPath());
        String dir = jpgFile.getParentFile().getName();
        String filename = jpgFile.getName();
        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/" + dir);
        File file = new File(configDir + "/files/" + dir + "/" + filename);
        if (file.exists()) {
            return dir + "/" + filename;
        }
        HttpReq.get(coverUrl)
                .then(res -> FileUtil.writeFromStream(res.bodyStream(), file));
        return dir + "/" + filename;
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
            );
        }

        String s = "(.*|\\[.*])( -? \\d+|\\[\\d+]|\\[\\d+.?[vV]\\d]|第\\d+[话話集]|\\[第?\\d+[话話集]]|\\[\\d+.?END]|[Ee][Pp]?\\d+)(.*)";

        items = items.stream()
                .filter(item -> {
                    try {
                        String itemTitle = item.getTitle();
                        String e = ReUtil.get(s, itemTitle, 2);
                        String episode = ReUtil.get("\\d+", e, 0);
                        if (StrUtil.isBlank(episode)) {
                            return false;
                        }
                        item.setEpisode(Integer.parseInt(episode) + offset);
                        item
                                .setReName(
                                        StrFormatter.format("{} S{}E{}",
                                                title,
                                                String.format("%02d", season),
                                                String.format("%02d", item.getEpisode()))
                                );
                        return true;
                    } catch (Exception e) {
                        log.error("解析rss视频集次出现问题");
                        log.debug(e.getMessage(), e);
                    }
                    return false;
                }).collect(Collectors.toList());
        return CollUtil.distinct(items, Item::getReName, false);
    }

    /**
     * 获取视频列表
     *
     * @param ani
     * @return
     */
    public static synchronized List<Item> getItems(Ani ani) {
        String url = ani.getUrl();
        String s = HttpReq.get(url)
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
