package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.MyMailAccount;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.ServerChanTypeEnum;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigUtil {

    public static final Config CONFIG = new Config();

    /*
      默认配置
     */
    static {
        String password = Md5Util.digestHex("admin");
        CONFIG.setSleep(15)
                .setMikanHost("https://mikanime.tv")
                .setTmdbApi("https://api.themoviedb.org")
                .setTmdbApiKey("")
                .setRenameSleep(0.5)
                .setGcSleep(10)
                .setRename(true)
                .setRss(true)
                .setRssTimeout(20)
                .setWatchErrorTorrent(true)
                .setDelayedDownload(0)
                .setFileExist(false)
                .setAwaitStalledUP(true)
                .setDelete(false)
                .setDeleteBackRSSOnly(false)
                .setDeleteFiles(false)
                .setOffset(false)
                .setTitleYear(false)
                .setAcronym(false)
                .setQuarter(false)
                .setQuarterMerge(false)
                .setAutoDisabled(false)
                .setDownloadPath(FilePathUtil.getAbsolutePath(new File("/Media/番剧")))
                .setOvaDownloadPath(FilePathUtil.getAbsolutePath(new File("/Media/剧场版")))
                .setHost("")
                .setDownload("qBittorrent")
                .setDownloadRetry(3)
                .setUsername("")
                .setPassword("")
                .setQbUseDownloadPath(false)
                .setRatioLimit(-2)
                .setSeedingTimeLimit(-2)
                .setInactiveSeedingTimeLimit(-2)
                .setSkip5(true)
                .setBackRss(false)
                .setCoexist(false)
                .setLogsMax(2048)
                .setDebug(false)
                .setProxy(false)
                .setProxyHost("")
                .setProxyPort(8080)
                .setProxyUsername("")
                .setProxyPassword("")
                .setDownloadCount(0)
                .setMail(false)
                .setMailAddressee("")
                .setMailImage(true)
                .setMailAccount(
                        new MyMailAccount()
                                .setHost("")
                                .setPort(25)
                                .setFrom("")
                                .setPass("")
                                .setSslEnable(false)
                                .setStarttlsEnable(false)
                )
                .setLogin(new Login()
                        .setUsername("admin")
                        .setPassword(password)
                )
                .setMultiLoginForbidden(true)
                .setLoginEffectiveHours(3)
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setImportExclude(false)
                .setEnabledExclude(false)
                .setTelegram(false)
                .setTelegramChatId("")
                .setTelegramBotToken("")
                .setTelegramApiHost("https://api.telegram.org")
                .setTelegramImage(true)
                .setTelegramFormat("")
                .setWebHook(false)
                .setTmdb(false)
                .setBgmJpName(false)
                .setTmdbId(false)
                .setTmdbLanguage("zh-CN")
                .setIpWhitelist(false)
                .setIpWhitelistStr("")
                .setWebHookBody("")
                .setWebHookUrl("")
                .setWebHookMethod("POST")
                .setSeasonName("Season 1")
                .setShowPlaylist(true)
                .setOmit(true)
                .setBgmToken("")
                .setApiKey("")
                .setWeekShow(true)
                .setScoreShow(true)
                .setDownloadNew(false)
                .setInnerIP(false)
                .setRenameTemplate("[${subgroup}] ${title} S${seasonFormat}E${episodeFormat}")
                .setRenameDelYear(false)
                .setRenameDelTmdbId(false)
                .setMessageList(List.of(
                        MessageEnum.DOWNLOAD_START,
                        MessageEnum.OMIT,
                        MessageEnum.ERROR
                ))
                .setVerifyLoginIp(false)
                .setServerChan(false)
                .setServerChanType(ServerChanTypeEnum.SERVER_CHAN.getType())
                .setServerChanSendKey("")
                .setServerChan3ApiUrl("")
                .setSystemMsg(false)
                .setAutoTrackersUpdate(false)
                .setTrackersUpdateUrls("https://cf.trackerslist.com/best.txt")
                .setMessageTemplate("${text}")
                .setAutoUpdate(false)
                .setAlist(false)
                .setAlistRetry(5)
                .setAlistTask(true)
                .setAlistPath("/")
                .setAlistOvaPath("")
                .setAlistHost("")
                .setAlistToken("")
                .setVersion("")
                .setBgmImage("large")
                .setCustomCss("")
                .setCustomJs("")
                .setCustomEpisode(false)
                .setCustomEpisodeStr(RenameUtil.REG_STR)
                .setCustomEpisodeGroupIndex(2)
                .setProvider("")
                .setUpload(true)
                .setUpLimit(0L)
                .setDlLimit(0L)
                .setExpirationTime(0L)
                .setOutTradeNo("")
                .setTryOut(false)
                .setVerifyExpirationTime(false)
                .setEmbyRefresh(false)
                .setEmbyApiKey("")
                .setEmbyRefreshViewIds(new ArrayList<>())
                .setEmbyDelayed(0L)
                .setProcrastinating(false)
                .setProcrastinatingDay(14)
                .setGithub("None")
                .setGithubToken("")
                .setAlistRefresh(false)
                .setAlistRefreshDelayed(0L)
                .setAlistUploadAsTask(false);
    }

    /**
     * 获取设置文件夹
     *
     * @return
     */
    public static File getConfigDir() {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "config");
        return new File(config).getAbsoluteFile();
    }

    /**
     * 获取设置文件
     *
     * @return
     */
    public static File getConfigFile() {
        File configDir = getConfigDir();
        return new File(configDir + File.separator + "config.json");
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
        BeanUtil.copyProperties(GsonStatic.fromJson(s, Config.class), CONFIG, CopyOptions
                .create()
                .setIgnoreNullValue(true));
        LogUtil.loadLogback();
        log.debug("加载配置文件 {}", configFile);
        TorrentUtil.load();
    }

    /**
     * 将设置保存到磁盘
     */
    public static synchronized void sync() {
        File configFile = getConfigFile();
        log.debug("保存配置 {}", configFile);
        try {
            String json = GsonStatic.toJson(CONFIG);
            // 校验json没有问题
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

}
