package ani.rss.util;

import ani.rss.entity.Ani;
import ani.rss.entity.BgmInfo;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class AniUtil {

    public static final List<Ani> ANI_LIST = new CopyOnWriteArrayList<>();

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
            FileUtil.writeUtf8String(GsonStatic.toJson(ANI_LIST), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        List<Ani> anis = GsonStatic.fromJsonList(s, Ani.class);
        for (Ani ani : anis) {
            Ani newAni = Ani.bulidAni();
            BeanUtil.copyProperties(ani, newAni, CopyOptions
                    .create()
                    .setIgnoreNullValue(true));
            ANI_LIST.add(newAni);
        }
        log.debug("加载订阅 共{}项", ANI_LIST.size());


        // 处理旧数据
        for (Ani ani : ANI_LIST) {
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
            for (Ani.BackRss rss : backRssList) {
                Integer offset = rss.getOffset();
                offset = ObjectUtil.defaultIfNull(offset, ani.getOffset());
                rss.setOffset(offset);
            }
        }
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
            FileUtil.rename(temp, configFile.getName(), true);
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
        return getAni(url, "", "", "");
    }

    /**
     * 获取动漫信息
     *
     * @param url
     * @return
     */
    public static Ani getAni(String url, String text, String type, String bgmUrl) {
        Config config = ConfigUtil.CONFIG;
        type = StrUtil.blankToDefault(type, "mikan");
        int season = 1;
        String title = "无标题";

        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);

        String subgroupid = "";
        for (String k : decodeParamMap.keySet()) {
            String v = decodeParamMap.get(k);
            if (k.equalsIgnoreCase("subgroupid")) {
                subgroupid = v;
            }
        }

        Ani ani = Ani.bulidAni();
        ani.setUrl(url.trim());

        if ("other".equals(type)) {
            if (StrUtil.isNotBlank(text)) {
                title = text;
            }

            if (StrUtil.isNotBlank(bgmUrl)) {
                String subjectId = BgmUtil.getSubjectId(new Ani().setBgmUrl(bgmUrl));
                log.info("subjectId: {}", subjectId);
                ani.setBgmUrl("https://bgm.tv/subject/" + subjectId);
            }
        } else {
            try {
                MikanUtil.getMikanInfo(ani, subgroupid);
            } catch (Exception e) {
                throw new RuntimeException("获取失败");
            }
        }

        try {
            BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani, true);

            String nameCn = bgmInfo.getNameCn();
            String name = bgmInfo.getName();

            Boolean bgmJpName = config.getBgmJpName();
            if (bgmJpName) {
                title = name;
            } else {
                title = StrUtil.blankToDefault(nameCn, name);
            }

            if (StrUtil.isBlank(title)) {
                title = "无标题";
            }

            season = bgmInfo.getSeason();
            int eps = bgmInfo.getEps();
            String subjectId = bgmInfo.getSubjectId();
            if (eps > 0) {
                eps = BgmUtil.getEpisodes(subjectId, 0).size();
            }
            String image = bgmInfo.getImage();

            LocalDateTime date = bgmInfo.getDate();
            ani.setTotalEpisodeNumber(eps)
                    .setOva(bgmInfo.getOva())
                    .setScore(bgmInfo.getScore())
                    .setYear(date.getYear())
                    .setMonth(date.getMonthValue())
                    .setDate(date.getDayOfMonth())
                    .setImage(image);
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(message, e);
            if (StrUtil.isNotBlank(bgmUrl)) {
                throw new RuntimeException("获取bgm信息失败");
            }
        }

        String image = ani.getImage();
        ani.setCover(saveJpg(image));

        String seasonReg = StrFormatter.format("第({}{1,2})季", ReUtil.RE_CHINESE);
        if (ReUtil.contains(seasonReg, title)) {
            season = Convert.chineseToNumber(ReUtil.get(seasonReg, title, 1));
            title = ReUtil.replaceAll(title, seasonReg, "").trim();
        }
        title = title.replace("剧场版", "").trim();

        Integer year = ani.getYear();

        Boolean downloadNew = config.getDownloadNew();
        Boolean titleYear = config.getTitleYear();
        Boolean tmdb = config.getTmdb();
        Boolean enabledExclude = config.getEnabledExclude();
        Boolean importExclude = config.getImportExclude();
        List<String> exclude = config.getExclude();

        if (titleYear && Objects.nonNull(year) && year > 0) {
            title = StrFormatter.format("{} ({})", title, year);
        }

        ani.setTitle(title);
        String themoviedbName = TmdbUtil.getName(ani);

        if (StrUtil.isNotBlank(themoviedbName) && tmdb) {
            title = themoviedbName;
        }

        if (importExclude) {
            exclude = new ArrayList<>(exclude);
            exclude.addAll(ani.getExclude());
            exclude = exclude.stream().distinct().collect(Collectors.toList());
            ani.setExclude(exclude);
        }

        title = RenameUtil.getName(title);

        ani
                .setDownloadNew(downloadNew)
                .setGlobalExclude(enabledExclude)
                .setType(type)
                .setSeason(season)
                .setTitle(title)
                .setThemoviedbName(themoviedbName);

        Boolean ova = ani.getOva();
        if (ova) {
            String ovaDownloadPath = config.getOvaDownloadPath();
            if (StrUtil.isNotBlank(ovaDownloadPath)) {
                ani.setDownloadPath(FileUtil.getAbsolutePath(ovaDownloadPath));
            }
        }

        String downloadPath = FileUtil.getAbsolutePath(TorrentUtil.getDownloadPath(ani).get(0));
        ani.setDownloadPath(downloadPath);

        log.debug("获取到动漫信息 {}", JSONUtil.formatJsonStr(GsonStatic.toJson(ani)));
        if (ani.getOva()) {
            return ani;
        }

        String s = HttpReq.get(url, true)
                .timeout(config.getRssTimeout() * 1000)
                .thenFunction(HttpResponse::body);
        List<Item> items = ItemsUtil.getItems(ani, s, new Item());
        if (items.isEmpty()) {
            return ani.setCustomEpisode(true);
        } else if (items.size() == 1) {
            // 自定义集数获取规则
            Double episode = items.get(0).getEpisode();
            if (episode == 1920 || episode == 1080) {
                return ani.setCustomEpisode(true);
            }
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
        return saveJpg(coverUrl, false);
    }

    /**
     * 保存图片
     *
     * @param coverUrl
     * @param isOverride 是否覆盖
     * @return
     */
    public static String saveJpg(String coverUrl, Boolean isOverride) {
        File configDir = ConfigUtil.getConfigDir();
        FileUtil.mkdir(configDir + "/files/");

        // 默认空图片
        String cover = "cover.png";
        if (!FileUtil.exist(configDir + "/files/" + cover)) {
            byte[] bytes = ResourceUtil.readBytes("image/cover.png");
            FileUtil.writeBytes(bytes, configDir + "/files/" + cover);
        }
        if (StrUtil.isBlank(coverUrl)) {
            return cover;
        }
        String filename = Md5Util.digestHex(coverUrl);
        filename = filename.charAt(0) + "/" + filename + "." + FileUtil.extName(URLUtil.getPath(coverUrl));
        FileUtil.mkdir(configDir + "/files/" + filename.charAt(0));
        File file = new File(configDir + "/files/" + filename);
        if (file.exists() && !isOverride) {
            return filename;
        }
        FileUtil.del(file);
        try {
            HttpReq.get(coverUrl, true)
                    .then(res -> FileUtil.writeFromStream(res.bodyStream(), file));
            return filename;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return cover;
        }
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

    public static String getBangumiId(Ani ani) {
        String url = ani.getUrl();
        if (StrUtil.isBlank(url)) {
            return "";
        }
        Map<String, String> decodeParamMap = HttpUtil.decodeParamMap(url, StandardCharsets.UTF_8);
        return decodeParamMap.get("bangumiId");
    }

}
