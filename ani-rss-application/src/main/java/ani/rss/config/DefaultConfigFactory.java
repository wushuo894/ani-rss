package ani.rss.config;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.enums.AniSortTypeEnum;
import ani.rss.enums.BgmTokenTypeEnum;
import ani.rss.enums.OffsetTypeEnum;
import ani.rss.util.other.RenameUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 全局配置默认值工厂，提供首次启动及加载配置前的初始化基准。
 * <p>
 * 负责平台相关的目录模板、功能默认值和认证标识初始化；
 * 已保存配置的读取、合并与持久化由配置管理流程负责。
 */
public class DefaultConfigFactory {
    /**
     * 首次启动的默认用户名，也用于识别尚未修改的默认登录信息。
     */
    public static final String DEFAULT_USERNAME = "admin";

    /**
     * 默认密码 admin 的 MD5 摘要，与登录配置中保存的密码格式一致。
     */
    public static final String DEFAULT_PASSWORD = SecureUtil.md5("admin");

    /**
     * 创建默认配置，不读取配置文件，也不创建下载目录。
     * <p>
     * 下载器连接初值可由运行环境提供；每次调用都会重新生成 API Key、
     * JWT 签名密钥及会话标识，调用方加载已有配置时再恢复已保存的数据。
     *
     * @return 包含平台目录模板、功能默认值和新认证标识的配置对象
     */
    public static Config create() {
        Config config = new Config();
        // Linux 等环境默认使用 /Media，macOS 和 Windows 使用用户下载目录。
        String rootPath = "/Media";

        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isMac() || osInfo.isWindows()) {
            rootPath = FileUtil.getUserHomePath() + "/Downloads";
        }

        // 统一生成绝对路径，分别承载普通番剧、OVA／剧场版和已完结番剧。
        String downloadPath = FileUtils.getAbsolutePath(new File(rootPath, "番剧"));
        String ovaDownloadPath = FileUtils.getAbsolutePath(new File(rootPath, "剧场版"));
        String completedPath = FileUtils.getAbsolutePath(new File(rootPath, "已完结番剧"));

        // 此处只替换根目录，保留 ${...} 占位符供具体订阅生成路径时展开。
        String downloadPathTemplate = StrFormatter.format("{}/${title}/Season ${season}", downloadPath);
        String ovaDownloadPathTemplate = StrFormatter.format("{}/${title}", ovaDownloadPath);
        String completedPathTemplate = StrFormatter.format("{}/${title}/Season ${season}", completedPath);

        String password = SecureUtil.md5("admin");

        // 全局通知正文模板，发送时替换事件、订阅进度、路径和 TMDB 信息。
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

        // 第三方接口使用独立随机密钥，不复用网页登录密码。
        String apiKey = RandomUtil.randomString(64).toLowerCase();

        // 从运行环境读取下载器连接初值；未指定时默认选择 qBittorrent 并留空凭据。
        String downloadToolType = SystemUtil.get("DOWNLOAD_TOOL_TYPE", "qBittorrent");
        String downloadToolHost = SystemUtil.get("DOWNLOAD_TOOL_HOST", "");
        String downloadToolUsername = SystemUtil.get("DOWNLOAD_TOOL_USERNAME", "");
        String downloadToolPassword = SystemUtil.get("DOWNLOAD_TOOL_PASSWORD", "");

        // 预置可能需要代理的域名；是否启用代理仍由 proxy 开关决定。
        String proxyList = """
                mikanani.me
                mikanime.tv
                anibt.net
                animes.garden
                nyaa.si
                acg.rip
                google.com
                tmdb.org
                themoviedb.org
                anilist.co
                wushuo.top
                bgm.tv
                bangumi.tv
                chii.in
                github.com
                raw.githubusercontent.com
                telegram.org
                """;
        return config.setRssSleepMinutes(15)
                .setMikanHost("https://mikanani.me")
                .setTmdbApi("https://api.themoviedb.org")
                .setTmdbApiKey("")
                .setTmdbImage("https://image.tmdb.org")
                .setTmdbAnime(true)
                .setRenameSleepSeconds(10)
                .setRename(true)
                .setRss(true)
                .setRssTimeout(20)
                .setCustomTags(new ArrayList<>())
                .setDelayedDownload(0)
                .setFileExist(false)
                .setAwaitStalledUP(true)
                .setDelete(false)
                .setDeleteStandbyRSSOnly(false)
                .setOffset(false)
                .setOffsetType(OffsetTypeEnum.AUTO_OFFSET)
                .setTitleYear(true)
                .setAutoDisabled(false)
                .setDownloadPathTemplate(downloadPathTemplate)
                .setOvaDownloadPathTemplate(ovaDownloadPathTemplate)
                .setDownloadToolHost(downloadToolHost)
                .setDownloadToolType(downloadToolType)
                .setDownloadRetry(3)
                .setDownloadToolUsername(downloadToolUsername)
                .setDownloadToolPassword(downloadToolPassword)
                .setQbUseDownloadPath(false)
                .setQbContentLayout("Original")
                .setRatioLimit(-2)
                .setSeedingTimeLimit(-2)
                .setInactiveSeedingTimeLimit(-2)
                .setSkip5(true)
                .setStandbyRss(false)
                .setCoexist(false)
                .setLogsMax(128)
                .setDebug(false)
                .setProcrastinatingMasterOnly(true)
                .setProxy(false)
                .setProxyHost("")
                .setProxyPort(8080)
                .setProxyUsername("")
                .setProxyPassword("")
                .setDownloadCount(0)
                // 默认登录信息与检测方法共用常量，密码保存 MD5 摘要。
                .setLogin(
                        new Login(DEFAULT_USERNAME, DEFAULT_PASSWORD)
                )
                .setMultiLoginForbidden(true)
                .setLoginEffectiveHours(3)
                .setExclude(List.of("720[Pp]", "\\d-\\d", "合集", "特别篇"))
                .setImportExclude(false)
                .setEnabledExclude(false)
                .setTmdb(true)
                .setBgmJpName(false)
                .setTmdbId(false)
                .setTmdbLanguage("zh-CN")
                .setTmdbRomaji(false)
                .setTmdbOriginalName(false)
                .setIpWhitelist(false)
                .setIpWhitelistStr("")
                .setOmit(true)
                .setBgmToken("")
                .setBgmTokenType(BgmTokenTypeEnum.INPUT)
                .setBgmAppID("")
                .setBgmAppSecret("")
                .setBgmRefreshToken("")
                .setBgmRedirectUri("")
                .setApiKey("")
                .setDownloadNew(false)
                .setInnerIP(false)
                .setRenameTemplate("[${subgroup}] ${title} S${seasonFormat}E${episodeFormat}")
                .setRenameDelYear(true)
                .setRenameDelTmdbId(true)
                .setPriorityKeywordsEnable(false)
                .setPriorityKeywords(new ArrayList<>())
                .setVerifyLoginIp(false)
                .setAutoTrackersUpdate(false)
                .setTrackersUpdateUrls("https://cf.trackerslist.com/best.txt")
                .setAutoUpdate(false)
                .setVersion("")
                .setBgmImageSize("medium")
                .setCustomCss("")
                .setCustomJs("")
                .setCustomEpisode(false)
                .setCustomEpisodeStr(RenameUtil.REG_STR)
                .setCustomEpisodeGroupIndex(2)
                .setUpload(true)
                .setUpLimit(0L)
                .setDlLimit(0L)
                .setExpirationTime(0L)
                .setOutTradeNo("")
                .setTryOut(false)
                .setVerifyExpirationTime(false)
                .setProcrastinating(false)
                .setProcrastinatingDay(14)
                .setGithubToken("")
                .setUpdateTotalEpisodeNumber(false)
                .setForceUpdateTotalEpisodeNumber(false)
                .setConfigBackup(false)
                .setConfigBackupDay(7)
                .setCompleted(false)
                .setCompletedPathTemplate(completedPathTemplate)
                .setNotificationTemplate(notificationTemplate)
                .setNotificationConfigList(new ArrayList<>())
                .setApiKey(apiKey)
                .setSortType(AniSortTypeEnum.SCORE)
                .setTmdbIdPlexMode(false)
                .setProxyList(proxyList)
                .setScrape(false)
                .setFollowDay(14)
                .setBangumiIniEnabled(false)
                .setMaxFileNameLength(0)
                .setLimitLoginAttempts(true)
                .setReverseProxyTrustIpList(List.of("127.0.0.1"))
                .setReverseProxyTrustIpListEnabled(false)
                .setSubtitleIndependentFolderEnabled(false)
                .setSubtitleIndependentFolderName("Subs")
                .setBgmApi("https://api.bgm.tv")
                .setAllowCors(false)
                .setUuid(UUID.randomUUID().toString())
                .setJwtKey(createJwtKey())
                .setTokenId(UUID.randomUUID().toString());
    }

    /**
     * 判断配置中的用户名和密码是否同时保持默认值。
     *
     * @param config 包含完整登录信息的配置对象
     * @return 用户名和密码摘要均为默认值时返回 true，否则返回 false
     */
    public static Boolean isDefaultLogin(Config config) {
        return isDefaultLogin(config.getLogin());
    }

    /**
     * 判断登录信息是否仍使用默认用户名和默认密码摘要。
     * <p>
     * 应在对外返回配置并清空密码之前调用；只修改用户名或密码任一项后，
     * 就不再满足默认登录信息的判断条件。
     *
     * @param login 包含非空用户名及 MD5 密码摘要的登录信息
     * @return 两项都与默认常量一致时返回 true，否则返回 false
     */
    public static Boolean isDefaultLogin(Login login) {
        String username = login.getUsername();
        String password = login.getPassword();
        // 必须同时匹配默认账号和密码摘要，不能与已脱敏的空密码比较。
        return username.equals(DefaultConfigFactory.DEFAULT_USERNAME) &&
                password.equals(DefaultConfigFactory.DEFAULT_PASSWORD);
    }

    /**
     * 生成 JWT 签名使用的随机密钥，并编码为可保存到配置文件的字符串。
     *
     * @return 由 32 字节（256 位）安全随机数据编码得到的 Base64 密钥
     */
    private static String createJwtKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        // 使用安全随机源填充密钥；Base64 仅用于字符串存储，不改变随机密钥内容。
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
