package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.Mikan;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    public static final List<Ani> ANI_LIST = new Vector<>();

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
            Ani newAni = Ani.bulidAni();
            BeanUtil.copyProperties(ani, newAni, CopyOptions
                    .create()
                    .setIgnoreNullValue(true));
            ANI_LIST.add(newAni);
        }
        log.debug("加载订阅 共{}项", ANI_LIST.size());


        // 处理旧数据
        for (Ani ani : ANI_LIST) {
            try {
                String cover = ani.getCover();
                if (ReUtil.contains("http(s*)://", cover)) {
                    cover = AniUtil.saveJpg(cover);
                    ani.setCover(cover);
                }
            } catch (Exception e) {
                String message = ExceptionUtil.getMessage(e);
                log.error(message);
                log.debug(message, e);
            }
            // 备用rss数据结构改变
            List<Ani.BackRss> backRssList = ani.getBackRssList();
            List<String> backRss = ani.getBackRss();
            if (backRssList.isEmpty() && !backRss.isEmpty()) {
                for (String rss : backRss) {
                    backRssList.add(
                            new Ani.BackRss()
                                    .setLabel("未知字幕组")
                                    .setUrl(rss)
                    );
                }
            }
        }
        AniUtil.sync();
    }

    /**
     * 将订阅配置保存到磁盘
     */
    public static synchronized void sync() {
        File configFile = getAniFile();
        log.debug("保存订阅 {}", configFile);
        try {
            String json = GSON.toJson(ANI_LIST);
            JsonArray jsonArray = GSON.fromJson(json, JsonArray.class);
            for (JsonElement jsonElement : jsonArray.asList()) {
                GSON.fromJson(jsonElement, Ani.class);
            }
            FileUtil.writeUtf8String(json, configFile);
            log.debug("保存成功 {}", configFile);
        } catch (Exception e) {
            log.error("保存失败 {}", configFile);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取动漫信息
     *
     * @param url
     * @return
     */
    public static Ani getAni(String url) {
        return getAni(url, "", "");
    }

    /**
     * 获取动漫信息
     *
     * @param url
     * @return
     */
    public static Ani getAni(String url, String text, String type) {
        type = StrUtil.blankToDefault(type, "mikan");
        int season = 1;
        String title = "无";

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

        String s = HttpReq.get(url, true)
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
        title = title.replace("剧场版", "").trim();

        Ani ani = Ani.bulidAni();

        if (List.of("nyaa", "dmhy").contains(type)) {
            if (StrUtil.isNotBlank(text)) {
                title = text;
            } else {
                Map<String, String> paramMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);
                title = paramMap.get("q");
                if (StrUtil.isBlank(text)) {
                    title = paramMap.get("keyword");
                }
            }
            Assert.notBlank(title, "标题获取失败，请手动填写");

            Mikan list = MikanUtil.list(title, new Mikan.Season());
            List<Mikan.Item> items = list.getItems();
            List<Ani> anis = new ArrayList<>();
            if (!items.isEmpty()) {
                Mikan.Item item = items.get(0);
                anis = item.getItems();
            }
            if (!anis.isEmpty()) {
                title = anis.get(0).getTitle();
                String url1 = anis.get(0).getUrl();
                ani.setBangumiId(new File(url1).getName());
            }
            Assert.notBlank(ani.getBangumiId(), "标题获取失败，请手动填写");
        } else {
            ani.setBangumiId(bangumiId);
        }

        try {
            MikanUtil.getMikanInfo(ani, subgroupid);
        } catch (Exception e) {
            throw new RuntimeException("获取失败");
        }

        String themoviedbName = getThemoviedbName(title);

        Config config = ConfigUtil.CONFIG;
        Boolean titleYear = config.getTitleYear();
        Boolean tmdb = config.getTmdb();
        Boolean enabledExclude = config.getEnabledExclude();
        Boolean importExclude = config.getImportExclude();
        List<String> exclude = config.getExclude();

        try {
            AniUtil.getBangumiInfo(ani, true, true);
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
        }

        Integer year = ani.getYear();

        if (StrUtil.isNotBlank(themoviedbName) && tmdb) {
            title = themoviedbName;
        }

        if (titleYear && Objects.nonNull(year) && year > 0) {
            title = StrFormatter.format("{} ({})", title, year);
            themoviedbName = StrFormatter.format("{} ({})", themoviedbName, year);
        }

        if (importExclude) {
            exclude = new ArrayList<>(exclude);
            exclude.addAll(ani.getExclude());
            exclude = exclude.stream().distinct().collect(Collectors.toList());
            ani.setExclude(exclude);
        }

        title = title.replace("1/2", "½");
        var ls = List.of("/", "\\", ":", "?", "*", "|", ">", "<", "\"");
        for (String l : ls) {
            title = title.replace(l, " ");
        }
        title = title.trim();

        ani
                .setGlobalExclude(enabledExclude)
                .setType(type)
                .setUrl(url.trim())
                .setSeason(season)
                .setTitle(title)
                .setThemoviedbName(themoviedbName);


        Boolean ova = ani.getOva();
        if (ova) {
            String ovaDownloadPath = config.getOvaDownloadPath();
            if (StrUtil.isNotBlank(ovaDownloadPath)) {
                ani.setDownloadPath(ovaDownloadPath);
            }
        }

        String downloadPath = TorrentUtil.getDownloadPath(ani).get(0)
                .toString()
                .replace("\\", "/");
        ani.setDownloadPath(downloadPath);

        log.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GSON.toJson(ani)));

        List<Item> items = getItems(ani, s);
        ani.setCurrentEpisodeNumber((int) items.stream().filter(it -> !it.getReName().endsWith(".5")).count());
        log.debug("获取到视频 共{}个", items.size());
        if (items.isEmpty() || ani.getOva()) {
            return ani;
        }
        // 自动推断剧集偏移
        if (config.getOffset()) {
            Double offset = -(items.stream()
                    .map(Item::getEpisode)
                    .min(Comparator.comparingDouble(i -> i))
                    .get() - 1);
            log.debug("自动获取到剧集偏移为 {}", offset);
            ani.setOffset(offset.intValue());
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
        HttpReq.get(coverUrl, true)
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
        List<String> match = ani.getMatch();
        Boolean ova = ani.getOva();

        int offset = ani.getOffset();
        int season = ani.getSeason();
        List<Item> items = new ArrayList<>();

        Document document = XmlUtil.readXML(xml);
        Node channel = document.getElementsByTagName("channel").item(0);
        NodeList childNodes = channel.getChildNodes();
        Config config = ConfigUtil.CONFIG;
        List<String> globalExcludeList = config.getExclude();
        Boolean globalExclude = ani.getGlobalExclude();

        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node item = childNodes.item(i);
            String nodeName = item.getNodeName();
            if (!nodeName.equals("item")) {
                continue;
            }
            String itemTitle = "";
            String torrent = "";
            String length = "";
            String infoHash = "";

            String size = "0MB";

            NodeList itemChildNodes = item.getChildNodes();
            for (int j = 0; j < itemChildNodes.getLength(); j++) {
                Node itemChild = itemChildNodes.item(j);
                String itemChildNodeName = itemChild.getNodeName();
                if (itemChildNodeName.equals("title")) {
                    itemTitle = itemChild.getTextContent();
                }

                if (itemChildNodeName.equals("enclosure")) {
                    NamedNodeMap attributes = itemChild.getAttributes();
                    String url = attributes.getNamedItem("url").getNodeValue();
                    length = attributes.getNamedItem("length").getNodeValue();
                    if (Long.parseLong(length) > 1) {
                        torrent = url;
                        infoHash = FileUtil.mainName(torrent);
                    }

                    String magnetReg = "^magnet\\:\\?xt=urn:btih\\:(\\w+)";
                    if (ReUtil.contains(magnetReg, url)) {
                        torrent = url;
                        infoHash = ReUtil.get(magnetReg, url, 1);
                    }
                }
                if (itemChildNodeName.equals("nyaa:infoHash")) {
                    infoHash = itemChild.getTextContent();
                }
                if (itemChildNodeName.equals("nyaa:size")) {
                    size = itemChild.getTextContent();
                    size = ReUtil.get("[\\d\\.]+", size, 0) + "MB";
                }

                if (itemChildNodeName.equals("link")) {
                    String link = itemChild.getTextContent();
                    if (!link.endsWith(".torrent")) {
                        continue;
                    }
                    torrent = link;
                }
            }

            if (StrUtil.isBlank(torrent)) {
                continue;
            }

            try {
                if (StrUtil.isNotBlank(length) && size.equals("0MB")) {
                    Double l = Long.parseLong(length) / 1024.0 / 1024;
                    size = NumberUtil.decimalFormat("0.00", l) + "MB";
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }

            if (StrUtil.isNotBlank(infoHash)) {
                infoHash = infoHash.toLowerCase();
            }

            Item newItem = new Item()
                    .setTitle(itemTitle)
                    .setReName(itemTitle)
                    .setTorrent(torrent)
                    .setInfoHash(infoHash)
                    .setSize(size);

            // 进行过滤
            if (exclude.stream().anyMatch(s -> ReUtil.contains(s, newItem.getTitle()))) {
                continue;
            }

            // 全局排除
            if (globalExclude) {
                if (globalExcludeList.stream().anyMatch(s -> ReUtil.contains(s, newItem.getTitle()))) {
                    continue;
                }
            }
            items.add(newItem);
        }

        // 匹配规则
        items = items.stream().filter(it -> {
            if (match.isEmpty()) {
                return true;
            }
            for (String string : match) {
                if (ReUtil.contains(string, it.getTitle())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

        if (ova) {
            return items;
        }

        String s = "(.*|\\[.*])( -? \\d+(\\.5)?|\\[\\d+(\\.5)?]|\\[\\d+(\\.5)?.?[vV]\\d]|第\\d+(\\.5)?[话話集]|\\[第?\\d+(\\.5)?[话話集]]|\\[\\d+(\\.5)?.?END]|[Ee][Pp]?\\d+(\\.5)?)(.*)";

        items = items.stream()
                .filter(item -> {
                    try {
                        String itemTitle = item.getTitle();
                        String e = ReUtil.get(s, itemTitle, 2);
                        String episode = ReUtil.get("\\d+(\\.5)?", e, 0);
                        if (StrUtil.isBlank(episode)) {
                            return false;
                        }
                        Boolean skip5 = config.getSkip5();
                        if (skip5) {
                            if (episode.endsWith(".5")) {
                                log.debug("{} 疑似 {} 剧集, 自动跳过", itemTitle, episode + ".5");
                                return false;
                            }
                        }

                        boolean is5 = Double.parseDouble(episode) - 0.5 == Double.valueOf(episode).intValue();

                        if (is5) {
                            item.setEpisode(Double.parseDouble(episode));
                        } else {
                            item.setEpisode(Double.parseDouble(episode) + offset);
                        }

                        String reName = StrFormatter.format("{} S{}E{}",
                                title,
                                String.format("%02d", season),
                                String.format("%02d", item.getEpisode().intValue()));

                        if (is5) {
                            reName = reName + ".5";
                        }

                        item
                                .setReName(reName);
                        return true;
                    } catch (Exception e) {
                        log.error("解析rss视频集次出现问题");
                        log.debug(e.getMessage(), e);
                    }
                    return false;
                }).collect(Collectors.toList());
        return CollUtil.distinct(items, Item::getReName, true);
    }

    /**
     * 获取视频列表
     *
     * @param ani
     * @return
     */
    public static synchronized List<Item> getItems(Ani ani) {
        String url = ani.getUrl();

        List<Item> items = new ArrayList<>();

        String s = HttpReq.get(url, true)
                .thenFunction(HttpResponse::body);
        items.addAll(getItems(ani, s)
                .stream()
                .peek(item -> {
                    item.setMaster(true)
                            .setSubgroup(ani.getSubgroup());
                })
                .collect(Collectors.toList()));

        Config config = ConfigUtil.CONFIG;
        Boolean qbRenameTitle = config.getQbRenameTitle();
        if (!qbRenameTitle || !config.getBackRss()) {
            return items;
        }

        List<Ani.BackRss> backRss = ani.getBackRssList();
        for (Ani.BackRss rss : backRss) {
            ThreadUtil.sleep(1000);
            s = HttpReq.get(rss.getUrl(), true)
                    .thenFunction(HttpResponse::body);
            items.addAll(getItems(ani, s)
                    .stream()
                    .peek(item -> {
                        item.setMaster(false)
                                .setSubgroup(StrUtil.blankToDefault(rss.getLabel(), "未知字幕组"));
                    })
                    .collect(Collectors.toList()));
        }
        items = CollUtil.distinct(items, Item::getReName, false);
        items.sort(Comparator.comparingDouble(Item::getEpisode));
        return items;
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

    public static void getBangumiInfo(Ani ani, Boolean ova, Boolean totalEpisode) {
        Integer totalEpisodeNumber = ObjectUtil.defaultIfNull(ani.getTotalEpisodeNumber(), 0);
        ani.setTotalEpisodeNumber(totalEpisodeNumber);
        if (totalEpisode) {
            if (totalEpisodeNumber > 0 && !ova) {
                return;
            }
        }

        String bangumiId = ani.getBangumiId();
        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(ani.getUrl(), StandardCharsets.UTF_8);
        if (StrUtil.isBlank(bangumiId)) {
            for (String k : decodeParamMap.keySet()) {
                String v = decodeParamMap.get(k);
                if (k.equalsIgnoreCase("bangumiId")) {
                    bangumiId = v;
                }
            }
            if (StrUtil.isNotBlank(bangumiId)) {
                ani.setBangumiId(bangumiId);
                AniUtil.sync();
            }
        }

        HttpReq.get(MikanUtil.getMikanHost() + "/Home/Bangumi/" + bangumiId, true)
                .then(res -> {
                    org.jsoup.nodes.Document document = Jsoup.parse(res.body());
                    Elements bangumiInfos = document.select(".bangumi-info");
                    String bgmUrl = "";
                    String year = "";
                    String month = "";
                    for (Element bangumiInfo : bangumiInfos) {
                        String string = bangumiInfo.ownText();
                        if (string.equals("Bangumi番组计划链接：")) {
                            bgmUrl = bangumiInfo.select("a").get(0).attr("href");
                        }
                        if (string.startsWith("放送开始：")) {
                            try {
                                String dateReg = "(\\d+)/(\\d+)/(\\d+)";
                                year = ReUtil.get(dateReg, string, 3);
                                month = ReUtil.get(dateReg, string, 1);
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                    if (StrUtil.isNotBlank(year) && StrUtil.isNotBlank(month) && ani.getYear() == 1970) {
                        ani.setYear(Integer.parseInt(year))
                                .setMonth(Integer.valueOf(month));
                    }

                    if (StrUtil.isBlank(bgmUrl) && !ova) {
                        return;
                    }

                    ani.setBgmUrl(bgmUrl);

                    HttpReq.get(bgmUrl, true)
                            .then(response -> {
                                org.jsoup.nodes.Document parse = Jsoup.parse(response.body());
                                Element inner = parse.selectFirst(".subject_tag_section");
                                if (Objects.nonNull(inner)) {
                                    Elements aa = inner.select("a");
                                    List<String> tags = new ArrayList<>();
                                    for (Element a : aa) {
                                        Element span = a.selectFirst("span");
                                        if (Objects.isNull(span)) {
                                            continue;
                                        }
                                        tags.add(span.ownText());
                                    }

                                    if (ova) {
                                        if (!tags.contains("TV") && (tags.contains("OVA") || tags.contains("剧场版"))) {
                                            ani.setOva(true);
                                        }
                                    }
                                }
                                for (Element element : parse.select(".tip")) {
                                    String s = element.ownText();
                                    if (!s.equals("话数:")) {
                                        continue;
                                    }
                                    try {
                                        String string = element.parent().ownText();
                                        if (!NumberUtil.isNumber(string)) {
                                            continue;
                                        }
                                        Integer ten = Integer.parseInt(string);
                                        if (!totalEpisodeNumber.equals(ten)) {
                                            ani.setTotalEpisodeNumber(ten);
                                            AniUtil.sync();
                                        }
                                        break;
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                    }
                                }
                            });
                });
    }

    /**
     * 获取番剧在tmdb的名称
     *
     * @param name
     * @return
     */
    public static String getThemoviedbName(String name) {
        try {
            return HttpReq.get("https://www.themoviedb.org/search", true)
                    .form("query", name)
                    .header("accept-language", "zh-CN")
                    .thenFunction(res -> {
                        org.jsoup.nodes.Document document = Jsoup.parse(res.body());
                        Element element = document.selectFirst(".title h2");
                        if (Objects.isNull(element)) {
                            return "";
                        }
                        String title = element.ownText();
                        title = title.replace("1/2", "½");
                        var ls = List.of("/", "\\", ":", "?", "*", "|", ">", "<", "\"");
                        for (String l : ls) {
                            title = title.replace(l, " ");
                        }
                        title = title.trim();
                        return StrUtil.blankToDefault(title, "");
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            return "";
        }
    }

}
