package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.StringEnum;
import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class ItemsUtil {

    static Cache<String, String> messageCache = CacheUtil.newFIFOCache(40960);

    /**
     * 获取视频列表
     *
     * @param ani
     * @return
     */
    public static synchronized List<Item> getItems(Ani ani) {
        String url = ani.getUrl();

        List<Item> items = new ArrayList<>();

        Config config = ConfigUtil.CONFIG;

        String s = HttpReq.get(url, true)
                .timeout(config.getRssTimeout() * 1000)
                .thenFunction(HttpResponse::body);
        String subgroup = StrUtil.blankToDefault(ani.getSubgroup(), "未知字幕组");
        items.addAll(ItemsUtil.getItems(ani, s, new Item().setSubgroup(subgroup))
                .stream()
                .peek(item -> item.setMaster(true))
                .collect(Collectors.toList()));

        if (!config.getBackRss()) {
            items.sort(Comparator.comparingDouble(Item::getEpisode));
            return items;
        }

        List<Ani.BackRss> backRss = ani.getBackRssList();
        for (Ani.BackRss rss : backRss) {
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
                    .collect(Collectors.toList()));
        }
        items = CollUtil.distinct(items, it -> it.getEpisode().toString(), false);
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
                if (itemChildNodeName.equals("nyaa:infoHash")) {
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
                }).collect(Collectors.toList());
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

        for (int i = min; i <= max; i++) {
            if (ArrayUtil.contains(array, i)) {
                continue;
            }
            list.add(i);
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

        ArrayList<String> sList = new ArrayList<>();

        for (Integer i : list) {
            String s = StrFormatter.format("缺少集数 {} S{}E{}", title, String.format("%02d", season), String.format("%02d", i));
            if (messageCache.containsKey(s)) {
                continue;
            }
            log.info(s);
            // 缓存一天 不重复发送
            messageCache.put(s, "1", TimeUnit.DAYS.toMillis(1));
            sList.add(s);
        }

        if (sList.isEmpty()) {
            return;
        }

        MessageUtil.send(config, ani, CollUtil.join(sList, "\n"), MessageEnum.OMIT);
    }

    public static int currentEpisodeNumber(Ani ani, List<Item> items) {
        if (items.isEmpty()) {
            return 0;
        }

        int currentEpisodeNumber;
        Boolean downloadNew = ani.getDownloadNew();
        if (downloadNew) {
            currentEpisodeNumber = items
                    .stream()
                    .filter(it -> it.getEpisode() == it.getEpisode().intValue())
                    .mapToInt(item -> item.getEpisode().intValue())
                    .max().orElse(0);
        } else {
            currentEpisodeNumber = (int) items
                    .stream()
                    .filter(it -> it.getEpisode() == it.getEpisode().intValue()).count();
        }
        return currentEpisodeNumber;
    }

}
