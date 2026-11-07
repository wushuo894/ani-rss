package ani.rss.util.other;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.config.DefaultAniFactory;
import ani.rss.entity.*;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.enums.OffsetTypeEnum;
import ani.rss.exception.ResultException;
import ani.rss.handle.JsonReader;
import ani.rss.handle.JsonWriter;
import ani.rss.offset.BaseOffset;
import ani.rss.service.ClearService;
import ani.rss.service.DownloadService;
import ani.rss.service.MikanService;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class AniUtil {

    private static final Config CONFIG = ConfigUtil.CONFIG;
    public static final List<Ani> ANI_LIST = new CopyOnWriteArrayList<>();
    public static final String FILE_NAME = "ani.v2.json";

    /**
     * 获取当前配置目录下的订阅数据文件。
     *
     * @return 指向 ani.v2.json 的文件对象
     */
    public static File getAniFile() {
        File configDir = ConfigUtil.getConfigDir();
        return new File(configDir + File.separator + FILE_NAME);
    }

    /**
     * 从磁盘加载订阅，替换内存中的订阅列表。
     * <p>
     * 加载时兼容旧版日期字段、缓存封面，并使用当前默认配置补全缺失字段。
     */
    public static void load() {
        File configFile = getAniFile();

        List<Ani> anis = JsonReader.getInstance(configFile)
                .toList(Ani.class, ANI_LIST);

        // 仅补全旧订阅中缺失的字段，保留用户已经保存的配置值。
        CopyOptions copyOptions = CopyOptions
                .create()
                .setIgnoreNullValue(true)
                .setOverride(false);

        ANI_LIST.clear();
        for (Ani ani : anis) {
            Date releaseDate = ani.getReleaseDate();
            if (Objects.isNull(releaseDate)) {
                releaseDate = new Date();
                // 处理旧的日期数据
                try {
                    Integer year = ani.getYear();
                    Integer month = ani.getMonth();
                    Integer date = ani.getDate();
                    String format = StrUtil.format("{}-{}-{}", year, month, date);
                    releaseDate = DateUtil.parse(format, DatePattern.NORM_DATE_PATTERN);
                } catch (Exception ignored) {
                }
                ani.setReleaseDate(releaseDate);
            }

            // 自动修补缺失的封面
            String image = ani.getImage();
            String cover = saveCover(image);
            ani.setCover(cover);

            Ani newAni = AniUtil.createAni();
            BeanUtil.copyProperties(newAni, ani, copyOptions);
            ANI_LIST.add(ani);
        }
        log.debug("加载订阅 共{}项", ANI_LIST.size());
    }

    /**
     * 将当前内存中的订阅列表同步保存到磁盘。
     * <p>
     * 使用同步锁避免并发写入；保存失败时记录日志。
     */
    public static synchronized void sync() {
        File configFile = getAniFile();
        log.debug("保存订阅 {}", configFile);
        try {
            // 写入到硬盘
            JsonWriter.getInstance(configFile)
                    .writer(ANI_LIST);
            log.debug("保存成功 {}", configFile);
        } catch (Exception e) {
            log.error("保存失败 {}", configFile);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 根据 RSS 来源解析并构建待添加的订阅对象。
     * <p>
     * 根据来源获取 Bangumi 链接和字幕组，补全作品信息及新增订阅默认配置，
     * 并在启用自动偏移时计算非剧场版订阅的主 RSS 和备用 RSS 偏移。
     *
     * @param dto 包含 RSS 地址、来源类型及可选 Bangumi 链接、字幕组和备用 RSS 的解析参数
     * @return 补全信息后的订阅对象，供预览或后续添加使用
     * @throws IllegalArgumentException RSS 地址为空，或解析后缺少 Bangumi 链接
     * @throws ResultException          获取蜜柑作品信息失败
     */
    public static Ani getAni(RssToAniDTO dto) {
        String url = dto.getUrl();
        String type = dto.getType();

        Boolean enable = Optional.of(dto)
                .map(RssToAniDTO::getEnable)
                .orElse(true);
        List<StandbyRss> standbyRssList = Optional.of(dto)
                .map(RssToAniDTO::getStandbyRssList)
                .orElse(new ArrayList<>());

        Assert.notBlank(url, "RSS地址 不能为空");

        type = StrUtil.blankToDefault(type, "mikan");

        Ani ani = AniUtil.createAni();
        ani.setUrl(url);
        ani.getStandbyRssList()
                .addAll(standbyRssList);

        Map<String, String> paramMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);

        // 各来源使用不同的 RSS 参数或页面信息提供 Bangumi 条目与字幕组。
        switch (type) {
            case "mikan":
                try {
                    String subgroup = dto.getSubgroup();
                    String bgmUrl = dto.getBgmUrl();
                    if (StrUtil.isAllBlank(subgroup, bgmUrl)) {
                        String subgroupId = MikanService.getSubgroupId(url);
                        MikanService.getMikanInfo(ani, subgroupId);
                    } else {
                        ani.setBgmUrl(bgmUrl)
                                .setSubgroup(subgroup);
                    }
                } catch (Exception e) {
                    throw ResultException.exception("获取失败");
                }
                break;
            case "ani-bt":
                if (paramMap.containsKey("bgmId")) {
                    String bgmUrl = "https://bgm.tv/subject/" + paramMap.get("bgmId");
                    ani.setBgmUrl(bgmUrl);
                }

                String subgroup = dto.getSubgroup();
                if (paramMap.containsKey("groupSlug") && StrUtil.isBlank(subgroup)) {
                    subgroup = paramMap.get("groupSlug");
                }
                ani.setSubgroup(subgroup);
                break;
            case "anime-garden":
                if (paramMap.containsKey("subject")) {
                    String bgmUrl = "https://bgm.tv/subject/" + paramMap.get("subject");
                    ani.setBgmUrl(bgmUrl);
                }
                if (paramMap.containsKey("fansub")) {
                    ani.setSubgroup(paramMap.get("fansub"));
                }
                break;
            default:
                String bgmUrl = dto.getBgmUrl();
                ani.setBgmUrl(bgmUrl);
        }

        String bgmUrl = ani.getBgmUrl();
        String subgroup = ani.getSubgroup();

        Assert.notBlank(bgmUrl, "bgmUrl 不能为空");

        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani, true);

        BgmUtil.toAni(bgmInfo, ani);

        // 只下载最新集
        Boolean downloadNew = CONFIG.getDownloadNew();
        // 默认启用全局排除
        Boolean enabledExclude = CONFIG.getEnabledExclude();
        // 默认导入全局排除
        Boolean importExclude = CONFIG.getImportExclude();
        // 全局排除
        List<String> exclude = CONFIG.getExclude();

        // 默认导入全局排除
        if (importExclude) {
            exclude = new ArrayList<>(exclude);
            exclude.addAll(ani.getExclude());
            exclude = exclude.stream().distinct().toList();
            ani.setExclude(exclude);
        }

        ani
                // 只下载最新集
                .setDownloadNew(downloadNew)
                // 是否启用全局排除
                .setGlobalExclude(enabledExclude)
                // type mikan or other
                .setType(type)
                .setEnable(enable);

        subgroup = StrUtil.blankToDefault(subgroup, "未知字幕组");

        if (subgroup.equals("未知字幕组")) {
            List<Item> items = ItemsUtil.getItems(ani, url, subgroup);
            subgroup = ItemsUtil.getSubgroup(items);
        }

        ani.setSubgroup(subgroup);

        log.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GsonStatic.toJson(ani)));
        if (ani.getOva()) {
            return ani;
        }

        // 自动推断剧集偏移
        if (CONFIG.getOffset()) {
            autoOffset(ani);
        }
        return ani;
    }

    /**
     * 使用全局选择的偏移策略，分别计算并回填主 RSS 和各备用 RSS 的集数偏移。
     *
     * @param ani 待更新偏移的订阅，策略计算过程中也可能调整其季编号
     */
    private static void autoOffset(Ani ani) {
        OffsetTypeEnum offsetType = CONFIG.getOffsetType();
        // 同一次处理中的主 RSS 和备用 RSS 共用一个偏移策略实例。
        BaseOffset baseOffset = ReflectUtil.newInstance(offsetType.getBaseOffsetClass());

        String url = ani.getUrl();
        Integer offset = autoOffset(ani, url, baseOffset);
        ani.setOffset(offset);

        List<StandbyRss> standbyRssList = ani.getStandbyRssList();
        for (StandbyRss standbyRss : standbyRssList) {
            String standbyRssUrl = standbyRss.getUrl();
            Integer standbyRssOffset = autoOffset(ani, standbyRssUrl, baseOffset);
            standbyRss.setOffset(standbyRssOffset);
        }
    }

    /**
     * 解析指定 RSS 的可用条目，并通过给定策略计算集数偏移。
     *
     * @param ani        用于 RSS 解析及目标集数计算的订阅，策略可能调整其季编号
     * @param rssUrl     待计算偏移的主 RSS 或备用 RSS 地址
     * @param baseOffset 当前处理所使用的偏移策略实例
     * @return 根据最小可用集数计算出的偏移量，RSS 没有可用条目时返回 0
     */
    private static Integer autoOffset(Ani ani, String rssUrl, BaseOffset baseOffset) {
        List<Item> items = ItemsUtil.getItems(ani, rssUrl, "");
        return baseOffset.getOffset(ani, items);
    }

    /**
     * 缓存订阅封面，优先复用已经下载的图片。
     *
     * @param coverUrl 封面图片地址，可为空
     * @return 相对于配置目录下 files 目录的封面路径，地址为空或下载异常时返回 cover.png
     */
    public static String saveCover(String coverUrl) {
        return saveCover(coverUrl, false);
    }

    /**
     * 将封面缓存到配置目录下的 files 目录，按图片 URL 的 MD5 命名并分目录存放。
     * <p>
     * 已缓存且未要求覆盖时直接返回路径；图片地址为空或下载异常时使用默认封面。
     *
     * @param coverUrl   封面图片地址，可为空
     * @param isOverride 是否删除已有缓存并重新下载
     * @return 相对于 files 目录的封面路径，例如 a/图片文件名.jpg 或默认封面 cover.png
     */
    public static String saveCover(String coverUrl, Boolean isOverride) {
        File configDir = ConfigUtil.getConfigDir();
        File filesDir = new File(configDir, "files");
        FileUtil.mkdir(filesDir);

        // 默认空图片
        String cover = "cover.png";
        File defaultFile = new File(filesDir, cover);
        if (!defaultFile.exists()) {
            try (InputStream inputStream = ResourceUtil.getStream("image/cover.png")) {
                FileUtil.writeFromStream(inputStream, defaultFile);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        if (StrUtil.isBlank(coverUrl)) {
            return cover;
        }

        String extName = FileUtil.extName(URLUtil.getPath(coverUrl));
        // 取url的md5作为文件名, 避免重复下载
        String filename = SecureUtil.md5(coverUrl) + "." + extName;

        File dir = new File(filesDir, String.valueOf(filename.charAt(0)));

        FileUtil.mkdir(dir);
        File file = new File(dir, filename);
        if (file.exists() && !isOverride) {
            return filename.charAt(0) + "/" + filename;
        }
        FileUtil.del(file);
        try {
            HttpReq.get(coverUrl)
                    .then(res -> FileUtil.writeFromStream(res.bodyStream(), file));
            return filename.charAt(0) + "/" + filename;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return cover;
        }
    }


    /**
     * 从蜜柑 RSS 地址的查询参数中提取作品 ID。
     *
     * @param ani 包含蜜柑 RSS 地址的订阅
     * @return bangumiId 参数值；RSS 地址为空时返回空字符串，未包含该参数时返回 null
     */
    public static String getBangumiId(Ani ani) {
        String url = ani.getUrl();
        if (StrUtil.isBlank(url)) {
            return "";
        }
        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);
        return decodeParamMap.get("bangumiId");
    }


    /**
     * 将符合完结条件的订阅文件迁移到配置的完结目录。
     * <p>
     * 仅处理已禁用、进度达到有效总集数的非剧场版订阅，且全局自动禁用、
     * 全局完结迁移和订阅自身的完结迁移开关均需开启。
     * 目标路径优先使用订阅自定义模板，迁移时同时调整下载器任务位置并清理旧目录。
     *
     * @param ani 待判断并迁移的订阅，方法在其副本上计算迁移路径
     */
    public static void completed(Ani ani) {
        // 使用副本计算迁移信息，避免迁移过程影响内存中的原订阅配置。
        ani = ObjectUtil.clone(ani);

        String title = ani.getTitle();
        boolean completed = ani.getCompleted();
        boolean ova = ani.getOva();
        boolean enable = ani.getEnable();
        int currentEpisodeNumber = ani.getCurrentEpisodeNumber();
        int totalEpisodeNumber = ani.getTotalEpisodeNumber();

        if (!completed) {
            // 未开启完结迁移
            return;
        }

        if (totalEpisodeNumber < 1) {
            // 总集数为空
            return;
        }

        if (currentEpisodeNumber < totalEpisodeNumber) {
            // 未完结
            return;
        }

        if (enable) {
            // 仍是启用的话 主RSS仍未完结
            return;
        }

        if (ova) {
            // 剧场版不进行迁移
            return;
        }

        Config config = ObjectUtil.clone(CONFIG);

        if (!config.getAutoDisabled()) {
            // 未开启自动禁用订阅
            return;
        }

        if (!config.getCompleted()) {
            // 未开启完结迁移
            return;
        }


        // 旧文件路径
        DownloadService downloadService = SpringUtil.getBean(DownloadService.class);
        String oldPath = downloadService.getDownloadPath(ani);

        List<TorrentsInfo> torrentsInfos = TorrentUtil.findTorrentsInfosByAni(ani);

        // 新文件路径
        String completedPathTemplate = ani.getCustomCompleted() ? ani.getCustomCompletedPathTemplate() : config.getCompletedPathTemplate();
        String newPath = downloadService.getDownloadPath(ani, completedPathTemplate);

        if (!FileUtil.exist(oldPath)) {
            // 旧位置不存在
            return;
        }

        FileUtil.mkdir(newPath);

        // 修改任务位置
        for (TorrentsInfo torrentsInfo : torrentsInfos) {
            // 修改保存位置
            TorrentUtil.setSavePath(torrentsInfo, newPath);
        }

        // 下载器修改保存位置后，短暂等待其处理，再迁移旧目录中剩余的文件。
        if (!torrentsInfos.isEmpty()) {
            ThreadUtil.sleep(3000);
        }

        File[] files = FileUtils.listFiles(oldPath);

        log.info("订阅已完结 {}, 移动已完结文件共 {} 个", title, files.length);

        for (File file : files) {
            if (!file.exists()) {
                continue;
            }
            // 移动文件
            log.info("移动 {} ==> {}", file, newPath);
            FileUtil.move(file, new File(newPath), true);
        }

        // 清理残留文件夹
        ClearService clearService = SpringUtil.getBean(ClearService.class);
        clearService.clearDir(oldPath);
    }

    /**
     * 通过默认配置工厂创建带有新 ID 和初始化字段的订阅对象。
     *
     * @return 按当前全局配置初始化自定义规则等默认值的新订阅对象
     */
    public static Ani createAni() {
        return DefaultAniFactory.create();
    }


}
