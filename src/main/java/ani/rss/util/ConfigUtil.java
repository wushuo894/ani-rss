package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.entity.MyMailAccount;
import ani.rss.enums.MessageEnum;
import ani.rss.enums.ServerChanTypeEnum;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Validator;
import cn.hutool.crypto.digest.MD5;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConfigUtil {

    public static final Config CONFIG = new Config();

    /*
      默认配置
     */
    static {
        String password = MD5.create().digestHex("admin");
        CONFIG.setSleep(15)
                .setMikanHost("https://mikanime.tv")
                .setRenameSleep(1.0)
                .setRename(true)
                .setRenameMinSize(100)
                .setRss(true)
                .setWatchErrorTorrent(true)
                .setDelayedDownload(0)
                .setFileExist(false)
                .setAwaitStalledUP(true)
                .setDelete(false)
                .setOffset(false)
                .setTitleYear(false)
                .setAcronym(false)
                .setQuarter(false)
                .setAutoDisabled(false)
                .setDownloadPath("")
                .setOvaDownloadPath("")
                .setHost("")
                .setDownload("qBittorrent")
                .setUsername("")
                .setPassword("")
                .setQbUseDownloadPath(false)
                .setSkip5(true)
                .setBackRss(false)
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
                )
                .setLogin(new Login()
                        .setUsername("admin")
                        .setPassword(password)
                )
                .setLoginEffectiveHours(3)
                .setExclude(List.of("720", "\\d-\\d", "合集", "特别篇"))
                .setImportExclude(false)
                .setEnabledExclude(false)
                .setTelegram(false)
                .setTelegramChatId("")
                .setTelegramBotToken("")
                .setTelegramApiHost("https://api.telegram.org")
                .setTelegramImage(true)
                .setWebHook(false)
                .setTmdb(false)
                .setTmdbId(false)
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
                .setRenameTemplate("${title} S${seasonFormat}E${episodeFormat}")
                .setRenameDelYear(false)
                .setRenameDelTmdbId(false)
                .setMessageList(List.of(
                        MessageEnum.DOWNLOAD_START,
                        MessageEnum.OMIT,
                        MessageEnum.ERROR
                ))
                .setVerifyLoginIp(true)
                .setServerChan(false)
                .setServerChanType(ServerChanTypeEnum.SERVER_CHAN.getType())
                .setServerChanSendKey("")
                .setServerChan3ApiUrl("")
                .setSystemMsg(false)
                .setAutoTrackersUpdate(false)
                .setTrackersUpdateUrls("https://cf.trackerslist.com/best.txt\nhttps://cdn.jsdelivr.net/gh/ngosang/trackerslist@master/trackers_all.txt")
                .setMessageTemplate("${text}");
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
        Boolean mail = CONFIG.getMail();
        MyMailAccount mailAccount = CONFIG.getMailAccount();
        String from = mailAccount.getFrom();
        String pass = mailAccount.getPass();
        String host = mailAccount.getHost();
        String mailAddressee = CONFIG.getMailAddressee();
        if (mail) {
            Assert.notBlank(host, "SMTP地址 不能为空");
            Assert.notBlank(pass, "发件人密码 不能为空");
            Assert.isTrue(Validator.isEmail(mailAddressee, true), "收件人 邮箱格式不正确");
            Assert.isTrue(Validator.isEmail(from, true), "发件人 邮箱格式不正确");
        }

        File configFile = getConfigFile();
        log.debug("保存配置 {}", configFile);
        try {
            String json = GsonStatic.toJson(CONFIG);
            // 校验json没有问题
            GsonStatic.fromJson(json, Config.class);
            FileUtil.writeUtf8String(json, configFile);
            LogUtil.loadLogback();
            log.debug("保存成功 {}", configFile);
        } catch (Exception e) {
            log.error("保存失败 {}", configFile);
            log.error(e.getMessage(), e);
        }
    }

}
