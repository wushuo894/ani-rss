package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Slf4j
public class ItemsUtil {

    /**
     * 获取视频列表
     *
     * @param ani
     * @return
     */
    public static synchronized List<Item> getItems(Ani ani) {
        String url = ani.getUrl();

        Config config = ConfigUtil.CONFIG;

        String s = HttpReq.get(url, true)
                .timeout(config.getRssTimeout() * 1000)
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    return res.body();
                });
        String subgroup = StrUtil.blankToDefault(ani.getSubgroup(), "未知字幕组");
        List<Item> items = new ArrayList<>(ItemsUtil.getItems(ani, s, new Item().setSubgroup(subgroup))
                .stream()
                .peek(item -> item.setMaster(true))
                .toList());

        if (!config.getStandbyRss()) {
            items.sort(Comparator.comparingDouble(Item::getEpisode));
            return items;
        }

        List<Ani.StandbyRss> standbyRssList = ani.getStandbyRssList();
        for (Ani.StandbyRss rss : standbyRssList) {
            ThreadUtil.sleep(1000);
            s = HttpReq.get(rss.getUrl(), true)
                    .timeout(config.getRssTimeout() * 1000)
                    .thenFunction(HttpResponse::body);
            subgroup = StrUtil.blankToDefault(rss.getLabel(), "未知字幕组");
            Ani clone = ObjUtil.clone(ani);
            clone.setOffset(rss.getOffset());
            items.addAll(ItemsUtil.getItems(clone, s, new Item().setSubgroup(subgroup))
                    .stream()
                    .peek(item -> item.setMaster(false))
                    .toList());
        }
        // 多字幕组共存模式
        Boolean coexist = config.getCoexist();
        if (coexist) {
            items = CollUtil.distinct(items, Item::getReName, false);
        } else {
            items = CollUtil.distinct(items, it -> it.getEpisode().toString(), false);
        }
        items.sort(Comparator.comparingDouble(Item::getEpisode));
        return items;
    }

    /**
     * 获取视频列表
     *
     * @param ani
     * @param xml
     * @return
     */
    public static List<Item> getItems(Ani ani, String xml, Item newItem) {
        List<String> exclude = ani.getExclude();
        List<String> match = ani.getMatch();

        List<Item> items = new ArrayList<>();

        Assert.notBlank(xml, "xml is blank");

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

            DateTime pubDate = null;

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
                    length = Optional.of(attributes)
                            .map(it -> it.getNamedItem("length"))
                            .map(Node::getNodeValue)
                            .orElse("1");

                    if (ReUtil.contains(StringEnum.MAGNET_REG, url)) {
                        torrent = url;
                        infoHash = ReUtil.get(StringEnum.MAGNET_REG, url, 1);
                    } else {
                        torrent = url;
                        infoHash = FileUtil.mainName(torrent);
                    }
                }

                if ("guid".equals(itemChildNodeName)) {
                    if (ReUtil.isMatch("^([a-z]|[0-9])+$", itemChild.getTextContent())) {
                        infoHash = itemChild.getTextContent();
                    }
                }

                if ("nyaa:infoHash".equals(itemChildNodeName)) {
                    infoHash = itemChild.getTextContent();
                }
                if (itemChildNodeName.equals("nyaa:size")) {
                    size = itemChild.getTextContent();
                }

                if (itemChildNodeName.equals("pubDate")) {
                    try {
                        pubDate = DateUtil.parse(itemChild.getTextContent());
                    } catch (Exception ignored) {
                    }
                }

                if (itemChildNodeName.equals("torrent")) {
                    try {
                        pubDate = DateUtil.parse(XmlUtil.getElement((Element) itemChild, "pubDate").getTextContent());
                    } catch (Exception ignored) {
                    }
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
                infoHash = URLUtil.decode(infoHash);
            }

            Item addNewItem = ObjectUtil.clone(newItem);

            addNewItem
                    .setEpisode(1.0)
                    .setTitle(itemTitle)
                    .setReName(itemTitle)
                    .setTorrent(torrent)
                    .setInfoHash(infoHash)
                    .setSize(size)
                    .setPubDate(pubDate);

            Function<String, String> map = s -> {
                String subgroup = ReUtil.get(StringEnum.SUBGROUP_REG_STR, s, 1);
                if (StrUtil.isBlank(subgroup)) {
                    return s;
                }
                if (subgroup.equals(newItem.getSubgroup())) {
                    return ReUtil.get(StringEnum.SUBGROUP_REG_STR, s, 2);
                }
                return "";
            };

            // 排除
            if (!exclude.isEmpty()) {
                if (exclude.stream().map(map).filter(StrUtil::isNotBlank).anyMatch(s -> ReUtil.contains(s, addNewItem.getTitle()))) {
                    continue;
                }
            }

            // 匹配
            if (!match.isEmpty()) {
                if (match.stream().map(map).filter(StrUtil::isNotBlank).anyMatch(s -> !ReUtil.contains(s, addNewItem.getTitle()))) {
                    continue;
                }
            }

            // 全局排除
            if (globalExclude) {
                if (globalExcludeList.stream().map(map).filter(StrUtil::isNotBlank).anyMatch(s -> ReUtil.contains(s, addNewItem.getTitle()))) {
                    continue;
                }
            }
            items.add(addNewItem);
        }

        items = items.stream()
                .filter(item -> {
                    try {
                        return RenameUtil.rename(ani, item);
                    } catch (Exception e) {
                        log.error("解析rss视频集次出现问题");
                        log.error(e.getMessage(), e);
                    }
                    return false;
                }).toList();
        return CollUtil.distinct(items, item -> item.getEpisode().toString(), true);
    }

    public static synchronized List<Integer> omitList(Ani ani, List<Item> items) {
        ArrayList<Integer> list = new ArrayList<>();
        Config config = ConfigUtil.CONFIG;
        Boolean omit = config.getOmit();
        if (!omit) {
            return list;
        }
        if (items.isEmpty()) {
            return list;
        }

        if (!ani.getOmit()) {
            return list;
        }

        Boolean ova = ani.getOva();
        if (ova) {
            return list;
        }

        int[] array = items.stream().mapToInt(o -> o.getEpisode().intValue()).distinct().toArray();
        int max = ArrayUtil.max(array);
        int min = ArrayUtil.min(array);
        if (max == min) {
            return list;
        }

        for (int ep = min; ep <= max; ep++) {
            if (ArrayUtil.contains(array, ep)) {
                // 包含该集
                continue;
            }
            if (50 < list.size()) {
                // 防止list过多
                return list;
            }
            list.add(ep);
        }
        return list;
    }

    /**
     * 检测是否缺集
     *
     * @param ani
     * @param items
     */
    public static synchronized void omit(Ani ani, List<Item> items) {
        Config config = ConfigUtil.CONFIG;
        List<Integer> list = omitList(ani, items);

        if (list.isEmpty()) {
            return;
        }

        // 缺少集数大于10个时可能是误判。因此不进行通知
        if (list.size() > 10) {
            return;
        }

        Integer season = ani.getSeason();
        String title = ani.getTitle();
        String id = ani.getId();

        ArrayList<String> sList = new ArrayList<>();

        for (Integer ep : list) {
            String s = StrFormatter.format("缺少集数 {} S{}E{}", title, String.format("%02d", season), String.format("%02d", ep));
            String key = StrFormatter.format("omit:{}:ep-{}", id, ep);
            if (MyCacheUtil.containsKey(key)) {
                // 一天内已经提醒过了
                continue;
            }
            log.info(s);
            // 缓存一天 不重复发送
            MyCacheUtil.put(key, s, TimeUnit.DAYS.toMillis(1));
            sList.add(s);
        }

        if (sList.isEmpty()) {
            return;
        }

        NotificationUtil.send(config, ani, CollUtil.join(sList, "\n"), NotificationStatusEnum.OMIT);
    }

    public static int currentEpisodeNumber(Ani ani, List<Item> items) {
        Config config = ConfigUtil.CONFIG;
        Boolean standbyRss = config.getStandbyRss();
        Boolean coexist = config.getCoexist();
        if (standbyRss && coexist) {
            // 开启多字幕组共存模式则只计算主rss集数
            items = items.stream()
                    .filter(Item::getMaster)
                    .toList();
        }

        // 过滤掉x.5集
        items = items
                .stream()
                .filter(it -> it.getEpisode() == it.getEpisode().intValue())
                .toList();

        if (items.isEmpty()) {
            return 0;
        }

        Boolean downloadNew = ani.getDownloadNew();
        if (downloadNew) {
            return items
                    .stream()
                    .mapToInt(item -> item.getEpisode().intValue())
                    .max()
                    .orElse(0);
        }
        return items.size();
    }

    /**
     * 摸鱼检测
     *
     * @param ani
     * @param items
     */
    public static void procrastinating(Ani ani, List<Item> items) {
        Config config = ConfigUtil.CONFIG;
        Boolean procrastinating = config.getProcrastinating();
        Integer procrastinatingDay = config.getProcrastinatingDay();
        if (!procrastinating) {
            return;
        }

        procrastinating = ani.getProcrastinating();

        if (!procrastinating) {
            return;
        }

        if (!AfdianUtil.verifyExpirationTime()) {
            log.info("未解锁捐赠, 无法使用摸鱼检测");
            return;
        }

        items.stream()
                .filter(Item::getMaster)
                .map(Item::getPubDate)
                .filter(Objects::nonNull)
                .mapToLong(Date::getTime)
                .max()
                .ifPresent(t -> {
                    DateTime date = DateUtil.date(t);
                    DateTime now = DateTime.now();

                    // 时间不对
                    if (now.getTime() <= t) {
                        return;
                    }
                    long day = DateUtil.between(date, now, DateUnit.DAY);
                    if (procrastinatingDay > day) {
                        // 未达到指定摸鱼时间
                        return;
                    }

                    String id = ani.getId();
                    String title = ani.getTitle();

                    String text = StrFormatter.format("检测到{}, 已摸鱼{}天", title, day);

                    String key = StrFormatter.format("procrastinating:{}", id);

                    if (MyCacheUtil.containsKey(key)) {
                        // 一天内已经提醒过了
                        return;
                    }

                    MyCacheUtil.put(key, text, TimeUnit.DAYS.toMillis(1));
                    NotificationUtil.send(config, ani, text, NotificationStatusEnum.PROCRASTINATING);
                });
    }

}
