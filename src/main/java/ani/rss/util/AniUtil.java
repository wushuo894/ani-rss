package ani.rss.util;

import ani.rss.action.ClearCacheAction;
import ani.rss.entity.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class AniUtil {

    public static final List<Ani> ANI_LIST = new CopyOnWriteArrayList<>();
    public static final String FILE_NAME = "ani.v2.json";

    /**
     * 获取订阅配置文件
     *
     * @return
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

        for (Ani ani : anis) {
            Ani newAni = Ani.createAni();
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
    public static Ani getAni(String url, String type, String bgmUrl) {
        Config config = ConfigUtil.CONFIG;
        type = StrUtil.blankToDefault(type, "mikan");
        String subgroupId = MikanUtil.getSubgroupId(url);

        Ani ani = Ani.createAni();
        ani.setUrl(url.trim());

        if ("mikan".equals(type)) {
            try {
                MikanUtil.getMikanInfo(ani, subgroupId);
            } catch (Exception e) {
                throw new RuntimeException("获取失败");
            }
        } else {
            ani.setBgmUrl(bgmUrl)
                    .setSubgroup("未知字幕组");
        }

        BgmInfo bgmInfo = BgmUtil.getBgmInfo(ani, true);

        BgmUtil.toAni(bgmInfo, ani);

        // 只下载最新集
        Boolean downloadNew = config.getDownloadNew();
        // 默认启用全局排除
        Boolean enabledExclude = config.getEnabledExclude();
        // 默认导入全局排除
        Boolean importExclude = config.getImportExclude();
        // 全局排除
        List<String> exclude = config.getExclude();

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
                .setType(type);


        List<StandbyRss> standbyRssList = ani.getStandbyRssList();

        boolean copyMasterToStandby = config.getCopyMasterToStandby();
        boolean standbyRss = config.getStandbyRss();
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
        if (config.getOffset()) {
            String s = HttpReq.get(url, true)
                    .timeout(config.getRssTimeout() * 1000)
                    .thenFunction(res -> {
                        HttpReq.assertStatus(res);
                        return res.body();
                    });
            List<Item> items = ItemsUtil.getItems(ani, s, new Item());
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
        String filename = SecureUtil.md5(coverUrl);
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


    /**
     * 获取蜜柑的bangumiId
     *
     * @param ani
     * @return
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
     * @param ani
     */
    public static void completed(Ani ani) {
        ani = ObjectUtil.clone(ani);

        String title = ani.getTitle();
        Boolean completed = ani.getCompleted();
        boolean ova = ani.getOva();
        boolean enable = ani.getEnable();
        int currentEpisodeNumber = ani.getCurrentEpisodeNumber();
        int totalEpisodeNumber = ani.getTotalEpisodeNumber();

        if (!completed) {
            // 未开启
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

        Config config = ObjectUtil.clone(ConfigUtil.CONFIG);

        boolean autoDisabled = config.getAutoDisabled();
        if (!autoDisabled) {
            // 未开启自动禁用订阅
            return;
        }

        completed = config.getCompleted();
        if (!completed) {
            // 未开启
            return;
        }

        Assert.isTrue(AfdianUtil.verifyExpirationTime(), "未解锁捐赠, 无法使用订阅完结迁移");

        String completedPathTemplate = config.getCompletedPathTemplate();

        Boolean customCompleted = ani.getCustomCompleted();
        if (customCompleted) {
            // 自定义完结迁移
            completedPathTemplate = ani.getCustomCompletedPathTemplate();
        }

        if (StrUtil.isBlank(completedPathTemplate)) {
            // 路径为空
            return;
        }

        // 旧文件路径
        File oldPath = TorrentUtil.getDownloadPath(ani, config);

        config.setDownloadPathTemplate(completedPathTemplate);
        // 因为临时修改下载位置模版以获取对应下载位置, 要关闭自定义下载位置
        ani.setCustomDownloadPath(false);

        // 新文件路径
        File newPath = TorrentUtil.getDownloadPath(ani, config);

        if (!oldPath.exists()) {
            // 旧文件不存在
            return;
        }

        FileUtil.mkdir(newPath);

        List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();

        for (TorrentsInfo torrentsInfo : torrentsInfos) {
            String downloadDir = torrentsInfo.getDownloadDir();
            if (!downloadDir.equals(FilePathUtil.getAbsolutePath(oldPath))) {
                // 旧位置不相同
                continue;
            }
            // 修改保存位置
            TorrentUtil.setSavePath(torrentsInfo, FilePathUtil.getAbsolutePath(newPath));
        }

        if (!torrentsInfos.isEmpty()) {
            ThreadUtil.sleep(3000);
        }

        File[] files = ObjectUtil.defaultIfNull(oldPath.listFiles(), new File[0]);

        log.info("订阅已完结 {}, 移动已完结文件共 {} 个", title, files.length);

        for (File file : files) {
            if (!file.exists()) {
                continue;
            }
            // 移动文件
            log.info("移动 {} ==> {}", file, newPath);
            FileUtil.move(file, newPath, true);
            // 清理残留文件夹
            ClearCacheAction.clearParentFile(file);
        }
    }


}
