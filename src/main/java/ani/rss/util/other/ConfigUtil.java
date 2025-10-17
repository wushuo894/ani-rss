package ani.rss.util.other;

import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.NotificationConfig;
import ani.rss.enums.BgmTokenTypeEnum;
import ani.rss.enums.SortTypeEnum;
import ani.rss.util.basic.FilePathUtil;
import ani.rss.util.basic.GsonStatic;
import ani.rss.util.basic.LogUtil;
import ani.rss.util.basic.MyURLUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.DynaBean;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class ConfigUtil {

    public static final Config CONFIG = new Config();
    public static final String FILE_NAME = "config.v2.json";

    /*
      默认配置
     */
    static {
        String rootPath = "/Media";

        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isMac()) {
            rootPath = FileUtil.getUserHomePath() + "/Movies";
        }

        String downloadPath = FilePathUtil.getAbsolutePath(new File(rootPath + "/番剧"));
        String ovaDownloadPath = FilePathUtil.getAbsolutePath(new File(rootPath + "/剧场版"));
        String completedPath = FilePathUtil.getAbsolutePath(new File(rootPath + "/已完结番剧"));

        String downloadPathTemplate = StrFormatter.format("{}/${letter}/${title}/Season ${season}", downloadPath);
        String ovaDownloadPathTemplate = StrFormatter.format("{}/${letter}/${title}", ovaDownloadPath);
        String completedPathTemplate = StrFormatter.format("{}/${letter}/${title}/Season ${season}", completedPath);

        String password = SecureUtil.md5("admin");

        String notificationTemplate = """
                ${emoji}${emoji}${emoji}
                事件类型: ${action}
                标题: ${title}
                评分: ${score}
                TMDB: ${tmdburl}
                TMDB标题: ${themoviedbName}
                BGM: ${bgmUrl}
                季: ${season}
                集: ${episode}
                字幕组: ${subgroup}
                进度: ${currentEpisodeNumber}/${totalEpisodeNumber}
                首播:  ${year}年${month}月${date}日
                事件: ${text}
                下载位置: ${downloadPath}
                TMDB集标题: ${episodeTitle}
                ${emoji}${emoji}${emoji}
                """;

        String apiKey = RandomUtil.randomString(64).toLowerCase();

        Map<String, String> env = System.getenv();

        String downloadToolType = env.getOrDefault("DOWNLOAD_TOOL_TYPE", "qBittorrent");
        String downloadToolHost = env.getOrDefault("DOWNLOAD_TOOL_HOST", "");
        String downloadToolUsername = env.getOrDefault("DOWNLOAD_TOOL_USERNAME", "");
        String downloadToolPassword = env.getOrDefault("DOWNLOAD_TOOL_PASSWORD", "");

        String proxyList = """
                mikanani.me
                mikanime.tv
                nyaa.si
                acg.rip
                google.com
                tmdb.org
                themoviedb.org
                anilist.co
                wushuo.top
                bgm.tv
                raw.githubusercontent.com
                github.com
                telegram.org
                """;

        CONFIG.setSleep(15)
                .setMikanHost("https://mikanani.me")
                .setTmdbApi("https://api.themoviedb.org")
                .setTmdbApiKey("")
                .setTmdbAnime(true)
                .setRenameSleepSeconds(10)
                .setGcSleep(10)
                .setRename(true)
                .setRss(true)
                .setRssTimeout(20)
                .setWatchErrorTorrent(true)
                .setCustomTags(new ArrayList<>())
                .setDelayedDownload(0)
                .setFileExist(false)
                .setAwaitStalledUP(true)
                .setDelete(false)
                .setDeleteStandbyRSSOnly(false)
                .setDeleteFiles(false)
                .setOffset(false)
                .setTitleYear(false)
                .setAutoDisabled(false)
                .setDownloadPathTemplate(downloadPathTemplate)
                .setOvaDownloadPathTemplate(ovaDownloadPathTemplate)
                .setDownloadToolHost(downloadToolHost)
                .setDownloadToolType(downloadToolType)
                .setDownloadRetry(3)
                .setDownloadToolUsername(downloadToolUsername)
                .setDownloadToolPassword(downloadToolPassword)
                .setQbUseDownloadPath(false)
                .setRatioLimit(-2)
                .setSeedingTimeLimit(-2)
                .setInactiveSeedingTimeLimit(-2)
                .setSkip5(true)
                .setStandbyRss(false)
                .setCoexist(false)
                .setLogsMax(2048)
                .setDebug(false)
                .setProcrastinatingMasterOnly(true)
                .setProxy(false)
                .setProxyHost("")
                .setProxyPort(8080)
                .setProxyUsername("")
                .setProxyPassword("")
                .setDownloadCount(0)
                .setLogin(new Login()
                        .setUsername("admin")
                        .setPassword(password)
                )
                .setMultiLoginForbidden(true)
                .setLoginEffectiveHours(3)
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setImportExclude(false)
                .setEnabledExclude(false)
                .setTmdb(false)
                .setBgmJpName(false)
                .setTmdbId(false)
                .setTmdbLanguage("zh-CN")
                .setTmdbRomaji(false)
                .setIpWhitelist(false)
                .setIpWhitelistStr("")
                .setShowPlaylist(true)
                .setOmit(true)
                .setBgmToken("")
                .setBgmTokenType(BgmTokenTypeEnum.INPUT)
                .setBgmAppID("")
                .setBgmAppSecret("")
                .setBgmRefreshToken("")
                .setBgmRedirectUri("")
                .setApiKey("")
                .setWeekShow(true)
                .setScoreShow(true)
                .setDownloadNew(false)
                .setInnerIP(false)
                .setRenameTemplate("[${subgroup}] ${title} S${seasonFormat}E${episodeFormat}")
                .setRenameDelYear(false)
                .setRenameDelTmdbId(false)
                .setPriorityKeywordsEnable(false)
                .setPriorityKeywords(new ArrayList<>())
                .setVerifyLoginIp(false)
                .setAutoTrackersUpdate(false)
                .setTrackersUpdateUrls("https://cf.trackerslist.com/best.txt")
                .setAutoUpdate(false)
                .setAlist(false)
                .setAlistRetry(5)
                .setAlistTask(true)
                .setAlistPath("/115/Media/番剧/${letter}/${title}/Season ${season}")
                .setAlistOvaPath("/115/Media/剧场版/${letter}/${title}")
                .setAlistHost("")
                .setAlistToken("")
                .setVersion("")
                .setBgmImage("large")
                .setCustomCss("")
                .setCustomJs("")
                .setCustomEpisode(false)
                .setCustomEpisodeStr(RenameUtil.REG_STR)
                .setCustomEpisodeGroupIndex(2)
                .setProvider("115 Cloud")
                .setUpload(true)
                .setUpLimit(0L)
                .setDlLimit(0L)
                .setExpirationTime(0L)
                .setOutTradeNo("")
                .setTryOut(false)
                .setVerifyExpirationTime(false)
                .setProcrastinating(false)
                .setProcrastinatingDay(14)
                .setGithub("None")
                .setGithubToken("")
                .setCustomGithub(false)
                .setCustomGithubUrl("")
                .setAlistRefresh(false)
                .setAlistRefreshDelayed(0L)
                .setUpdateTotalEpisodeNumber(false)
                .setAlistDownloadTimeout(60)
                .setAlistDownloadRetryNumber(5L)
                .setTvShowNfo(false)
                .setSeasonNfo(false)
                .setConfigBackup(false)
                .setConfigBackupDay(7)
                .setShowLastDownloadTime(false)
                .setCompleted(false)
                .setCompletedPathTemplate(completedPathTemplate)
                .setNotificationTemplate(notificationTemplate)
                .setNotificationConfigList(new ArrayList<>())
                .setApiKey(apiKey)
                .setCopyMasterToStandby(false)
                .setSortType(SortTypeEnum.SCORE)
                .setTmdbIdPlexMode(false)
                .setProxyList(proxyList);
    }

    /**
     * 获取设置文件夹
     *
     * @return
     */
    public static File getConfigDir() {
        Map<String, String> env = System.getenv();
        if (env.containsKey("CONFIG")) {
            return new File(env.get("CONFIG"));
        }

        File file = new File("config").getAbsoluteFile();

        if (file.exists()) {
            return file;
        }

        OsInfo osInfo = SystemUtil.getOsInfo();

        if (osInfo.isWindows() || osInfo.isMac()) {
            file = new File(FileUtil.getUserHomePath() + "/ani-rss");
        }

        return file;
    }

    /**
     * 获取设置文件
     *
     * @return
     */
    public static File getConfigFile() {
        File configDir = getConfigDir();
        return new File(configDir + File.separator + FILE_NAME);
    }

    /**
     * 加载设置
     */
    public static synchronized void load() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(GsonStatic.toJson(CONFIG), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);

        CopyOptions copyOptions = CopyOptions
                .create()
                .setIgnoreNullValue(true);
        BeanUtil.copyProperties(GsonStatic.fromJson(s, Config.class), CONFIG, copyOptions);
        format(CONFIG);
        LogUtil.loadLogback();
        log.debug("加载配置文件 {}", configFile);
        TorrentUtil.load();
        ThreadUtil.execute(() -> {
            try {
                AfdianUtil.verify();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    /**
     * 将设置保存到磁盘
     */
    public static synchronized void sync() {
        File configFile = getConfigFile();
        log.debug("保存配置 {}", configFile);
        try {
            ConfigUtil.format(CONFIG);
            String json = GsonStatic.toJson(CONFIG);
            File temp = new File(configFile + ".temp");
            FileUtil.del(temp);
            FileUtil.writeUtf8String(json, temp);
            FileUtil.rename(temp, configFile.getName(), true);
            LogUtil.loadLogback();
            log.debug("保存成功 {}", configFile);
        } catch (Exception e) {
            log.error("保存失败 {}", configFile);
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 备份
     */
    public static synchronized void backup() {
        Boolean configBackup = CONFIG.getConfigBackup();
        if (!configBackup) {
            return;
        }

        clearBackup();

        File configDir = getConfigDir();
        File backupDir = new File(configDir + "/backup");

        String date = DateUtil.format(new Date(), DatePattern.NORM_DATE_PATTERN);
        File backupFile = new File(backupDir + "/" + date + ".zip");

        if (backupFile.exists()) {
            return;
        }

        log.info("正在备份设置 {}", backupFile.getName());

        List<File> backupFiles = Stream.of(
                        "files", "torrents", "database.db",
                        AniUtil.FILE_NAME, ConfigUtil.FILE_NAME
                )
                .map(s -> configDir + "/" + s)
                .map(File::new)
                .filter(File::exists)
                .toList();

        try {
            ZipUtil.zip(backupFile, StandardCharsets.UTF_8, true, pathname -> {
                if (pathname.isFile()) {
                    return !List.of(".DS_Store", ".DS_Store@SynoResource")
                            .contains(pathname.getName());
                }
                File[] files = pathname.listFiles();
                return !ArrayUtil.isEmpty(files);
            }, backupFiles.toArray(new File[0]));

            log.info("备份设置成功 {}", backupFile.getName());
        } catch (Exception e) {
            log.error("备份失败 {}", backupFile.getName());
            log.error(e.getMessage(), e);
        }
    }


    /**
     * 清理备份
     */
    public static synchronized void clearBackup() {
        Integer configBackupDay = CONFIG.getConfigBackupDay();

        // 过期时间
        long expirationTime = DateUtil.offsetDay(new Date(), -configBackupDay).getTime();

        File configDir = getConfigDir();
        File backupDir = new File(configDir + "/backup");
        if (!backupDir.exists()) {
            return;
        }

        File[] files = backupDir.listFiles();
        if (ArrayUtil.isEmpty(files)) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            String extName = FileUtil.extName(file);
            if (!extName.equals("zip")) {
                continue;
            }
            String mainName = FileUtil.mainName(file);
            try {
                long time = DateUtil.parse(mainName, DatePattern.NORM_DATE_PATTERN).getTime();
                if (time > expirationTime) {
                    continue;
                }
                log.info("{} 备份已过期, 自动删除", file.getName());
                FileUtil.del(file);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取下载工具的密码
     *
     * @return
     */
    public static String getDownloadToolPassword() {
        Map<String, String> env = System.getenv();
        return env.getOrDefault("DOWNLOAD_TOOL_PASSWORD", "");
    }

    /**
     * 处理设置内的url与文件路径标准
     *
     * @param config
     */
    public static void format(Config config) {
        formatPath(config);
        formatUrl(config);

        String messageTemplate = config.getNotificationTemplate();
        config.setNotificationTemplate(messageTemplate.trim());

        NotificationConfig newNotificationConfig = NotificationConfig.createNotificationConfig();

        List<NotificationConfig> notificationConfigList = config.getNotificationConfigList();

        CopyOptions copyOptions = CopyOptions
                .create()
                .setIgnoreNullValue(true)
                // 禁止覆盖模式 仅补全null值
                .setOverride(false);

        for (NotificationConfig notificationConfig : notificationConfigList) {
            BeanUtil.copyProperties(newNotificationConfig, notificationConfig, copyOptions);
        }
    }

    /**
     * 处理url
     *
     * @param config
     */
    public static void formatUrl(Config config) {
        List<Func1<Config, String>> func1List = List.of(
                Config::getDownloadToolHost,
                Config::getAlistHost,
                Config::getMikanHost,
                Config::getTmdbApi,
                Config::getCustomGithubUrl
        );

        DynaBean dynaBean = DynaBean.create(config);

        for (Func1<Config, String> func1 : func1List) {
            String fieldName = LambdaUtil.getFieldName(func1);
            String v = func1.callWithRuntimeException(config);
            v = MyURLUtil.getUrlStr(v);
            dynaBean.set(fieldName, v);
        }
    }

    /**
     * 处理文件路径
     *
     * @param config
     */
    public static void formatPath(Config config) {
        List<Func1<Config, String>> func1List = List.of(
                Config::getDownloadPathTemplate,
                Config::getOvaDownloadPathTemplate,
                Config::getCompletedPathTemplate,
                Config::getAlistPath,
                Config::getAlistOvaPath
        );

        DynaBean dynaBean = DynaBean.create(config);

        for (Func1<Config, String> func1 : func1List) {
            String fieldName = LambdaUtil.getFieldName(func1);
            String v = func1.callWithRuntimeException(config);
            v = FilePathUtil.getAbsolutePath(v);
            dynaBean.set(fieldName, v);
        }
    }

}
