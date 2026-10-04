package ani.rss.config;

import ani.rss.commons.FileUtils;
import ani.rss.entity.Config;
import ani.rss.entity.Login;
import ani.rss.enums.AniSortTypeEnum;
import ani.rss.enums.BgmTokenTypeEnum;
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

public class DefaultConfigFactory {
    public static Config create() {
        Config config = new Config();
        String rootPath = "/Media";

        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isMac() || osInfo.isWindows()) {
            rootPath = FileUtil.getUserHomePath() + "/Downloads";
        }

        String downloadPath = FileUtils.getAbsolutePath(new File(rootPath, "番剧"));
        String ovaDownloadPath = FileUtils.getAbsolutePath(new File(rootPath, "剧场版"));
        String completedPath = FileUtils.getAbsolutePath(new File(rootPath, "已完结番剧"));

        String downloadPathTemplate = StrFormatter.format("{}/${title}/Season ${season}", downloadPath);
        String ovaDownloadPathTemplate = StrFormatter.format("{}/${title}", ovaDownloadPath);
        String completedPathTemplate = StrFormatter.format("{}/${title}/Season ${season}", completedPath);

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

        String downloadToolType = SystemUtil.get("DOWNLOAD_TOOL_TYPE", "qBittorrent");
        String downloadToolHost = SystemUtil.get("DOWNLOAD_TOOL_HOST", "");
        String downloadToolUsername = SystemUtil.get("DOWNLOAD_TOOL_USERNAME", "");
        String downloadToolPassword = SystemUtil.get("DOWNLOAD_TOOL_PASSWORD", "");

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
                .setLogin(new Login()
                        .setUsername("admin")
                        .setPassword(password)
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
                .setRenameDelYear(false)
                .setRenameDelTmdbId(false)
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
                .setProvider("115 Open")
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
                .setOpenListDownloadTimeout(60)
                .setOpenListDownloadRetryNumber(5L)
                .setConfigBackup(false)
                .setConfigBackupDay(7)
                .setCompleted(false)
                .setCompletedPathTemplate(completedPathTemplate)
                .setNotificationTemplate(notificationTemplate)
                .setNotificationConfigList(new ArrayList<>())
                .setApiKey(apiKey)
                .setCopyMasterToStandby(false)
                .setSortType(AniSortTypeEnum.SCORE)
                .setTmdbIdPlexMode(false)
                .setProxyList(proxyList)
                .setScrape(false)
                .setFollowDay(14)
                .setBangumiIniEnabled(false)
                .setReplace(false)
                .setMaxFileNameLength(0)
                .setLimitLoginAttempts(true)
                .setReverseProxyTrustIpList(List.of("127.0.0.1"))
                .setReverseProxyTrustIpListEnabled(false)
                .setSubtitleIndependentFolderEnabled(false)
                .setSubtitleIndependentFolderName("Subs")
                .setBgmApi("https://api.bgm.tv")
                .setAutoStart(false)
                .setAllowCors(false)
                .setUuid(UUID.randomUUID().toString())
                .setJwtKey(createJwtKey())
                .setTokenId(UUID.randomUUID().toString());
    }

    private static String createJwtKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32]; // 32字节 = 256 bits
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
