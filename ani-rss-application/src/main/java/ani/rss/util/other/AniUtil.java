package ani.rss.util.other;

import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.*;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.exception.ResultException;
import ani.rss.service.ClearService;
import ani.rss.service.DownloadService;
import ani.rss.service.MikanService;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import wushuo.tmdb.api.entity.Tmdb;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class AniUtil {

    private static final Config CONFIG = ConfigUtil.CONFIG;
    public static final List<Ani> ANI_LIST = new CopyOnWriteArrayList<>();
    public static final String FILE_NAME = "ani.v2.json";

    /**
     * 获取订阅配置文件
     *
     * @return 配置文件
     */
    public static File getAniFile() {
        File configDir = ConfigUtil.getConfigDir();
        return new File(configDir + File.separator + FILE_NAME);
    }

    /**
     * 加载订阅
     */
    public static void load() {
        File configFile = getAniFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(GsonStatic.toJson(ANI_LIST), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        List<Ani> anis = GsonStatic.fromJsonList(s, Ani.class);

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
            saveCover(image);

            Ani newAni = AniUtil.createAni();
            BeanUtil.copyProperties(newAni, ani, copyOptions);
            ANI_LIST.add(ani);
        }
        log.debug("加载订阅 共{}项", ANI_LIST.size());
    }

    /**
     * 将订阅配置保存到磁盘
     */
    public static synchronized void sync() {
        File configFile = getAniFile();
        log.debug("保存订阅 {}", configFile);
        try {
            String json = GsonStatic.toJson(ANI_LIST);
            File temp = new File(configFile + ".temp");
            FileUtil.del(temp);
            FileUtil.writeUtf8String(json, temp);
            FileUtils.move(temp.toPath(), configFile.toPath());
            log.debug("保存成功 {}", configFile);
        } catch (Exception e) {
            log.error("保存失败 {}", configFile);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取动漫信息
     *
     * @param dto DTO
     * @return 订阅
     */
    public static Ani getAni(RssToAniDTO dto) {
        String url = dto.getUrl();
        String type = dto.getType();
        Boolean enable = dto.getEnable();
        enable = ObjectUtil.defaultIfNull(enable, true);

        Assert.notBlank(url, "RSS地址 不能为空");

        type = StrUtil.blankToDefault(type, "mikan");

        Ani ani = AniUtil.createAni();
        ani.setUrl(url);

        Map<String, String> paramMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);

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

        List<StandbyRss> standbyRssList = ani.getStandbyRssList();

        boolean copyMasterToStandby = CONFIG.getCopyMasterToStandby();
        boolean standbyRss = CONFIG.getStandbyRss();
        if (copyMasterToStandby && standbyRss) {
            StandbyRss copyStandbyRss = new StandbyRss()
                    .setUrl(url.trim())
                    .setOffset(0)
                    .setLabel(ani.getSubgroup());
            standbyRssList.add(copyStandbyRss);
        }

        log.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GsonStatic.toJson(ani)));
        if (ani.getOva()) {
            return ani;
        }

        // 自动推断剧集偏移
        if (CONFIG.getOffset()) {
            List<Item> items = ItemsUtil.getItems(ani, url, subgroup);
            if (items.isEmpty()) {
                return ani;
            }
            Double offset = -(items.stream()
                    .map(Item::getEpisode)
                    .min(Comparator.comparingDouble(i -> i))
                    .get() - 1);
            log.debug("自动获取到剧集偏移为 {}", offset);
            ani.setOffset(offset.intValue());

            for (StandbyRss rss : standbyRssList) {
                rss.setOffset(offset.intValue());
            }
        }
        return ani;
    }


    public static String saveCover(String coverUrl) {
        return saveCover(coverUrl, false);
    }

    /**
     * 保存图片
     *
     * @param coverUrl   图片链接
     * @param isOverride 是否覆盖
     * @return 相对位置
     */
    public static String saveCover(String coverUrl, Boolean isOverride) {
        File configDir = ConfigUtil.getConfigDir();
        File filesDir = new File(configDir, "files");
        FileUtil.mkdir(filesDir);

        // 默认空图片
        String cover = "cover.png";
        File defaultFile = Path.of(filesDir.toString(), cover).toFile();
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

        File dir = new File(filesDir.toString(), String.valueOf(filename.charAt(0)));

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
     * 获取蜜柑的bangumiId
     *
     * @param ani 订阅
     * @return bangumiId
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
     * 订阅完结迁移
     *
     * @param ani 订阅
     */
    public static void completed(Ani ani) {
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

    public static Ani createAni() {
        Ani newAni = new Ani();
        return newAni
                .setId(UUID.randomUUID().toString())
                .setMikanTitle("")
                .setStandbyRssList(new ArrayList<>())
                .setOffset(0)
                .setReleaseDate(new DateTime())
                .setEnable(true)
                .setOva(false)
                .setScore(0.0)
                .setLastDownloadTime(0L)
                .setImage("")
                .setThemoviedbName("")
                .setCustomDownloadPath(false)
                .setCustomDownloadPathTemplate("")
                .setGlobalExclude(false)
                .setCurrentEpisodeNumber(0)
                .setTotalEpisodeNumber(0)
                .setMatch(List.of())
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setBgmUrl("")
                .setSubgroup("")
                .setCustomEpisode(CONFIG.getCustomEpisode())
                .setCustomEpisodeStr(CONFIG.getCustomEpisodeStr())
                .setCustomEpisodeGroupIndex(CONFIG.getCustomEpisodeGroupIndex())
                .setOmit(true)
                .setDownloadNew(false)
                .setNotDownload(new ArrayList<>())
                .setTmdb(
                        new Tmdb()
                                .setId("")
                                .setName("")
                                .setDate(new Date())
                )
                .setUpload(CONFIG.getUpload())
                .setProcrastinating(true)
                .setCustomRenameTemplate(CONFIG.getRenameTemplate())
                .setCustomRenameTemplateEnable(false)
                .setCustomPriorityKeywordsEnable(false)
                .setCustomPriorityKeywords(new ArrayList<>())
                .setMessage(true)
                .setCustomUploadPathTarget("")
                .setCustomUploadEnable(false)
                .setCompleted(true)
                .setCustomCompleted(false)
                .setCustomCompletedPathTemplate("")
                .setCustomTags(new ArrayList<>())
                .setCustomTagsEnable(false);
    }


}
