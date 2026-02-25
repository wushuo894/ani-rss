package ani.rss.config;

import ani.rss.download.BaseDownload;
import ani.rss.entity.About;
import ani.rss.entity.Config;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.UpdateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CronConfig {
    @Scheduled(cron = "0 0 0 * * *")
    public void backupConfig() {
        ConfigUtil.backup();
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void autoTrackersUpdate() {
        Config config = ConfigUtil.CONFIG;
        Boolean autoTrackersUpdate = config.getAutoTrackersUpdate();
        if (!autoTrackersUpdate) {
            // 未开启自动更新  Trackers
            return;
        }
        log.info("定时任务 开始更新 Trackers");
        try {
            updateTrackers(config);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void autoUpdate() {
        Config config = ConfigUtil.CONFIG;
        Boolean autoUpdate = config.getAutoUpdate();
        if (!autoUpdate) {
            // 未开启 自动更新
            return;
        }
        log.info("定时任务 自动更新");
        try {
            About about = UpdateUtil.about();
            Boolean update = about.getUpdate();
            autoUpdate = about.getAutoUpdate();
            if (!autoUpdate) {
                // 禁止非跨小版本的自动更新
                return;
            }
            if (update) {
                log.info("检测到可更新版本 v{}", about.getLatest());
            }
            UpdateUtil.update(about);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void updateTrackers(Config config) {
        String trackersUpdateUrls = config.getTrackersUpdateUrls();
        Assert.notBlank(trackersUpdateUrls, "Trackers更新地址 为空");

        Set<String> urls = StrUtil.split(trackersUpdateUrls, "\n").stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Assert.isTrue(!urls.isEmpty(), "Trackers更新地址 为空");

        Set<String> trackers = new HashSet<>();

        for (String url : urls) {
            log.info("获取 tracker {}", url);
            HttpReq.get(url)
                    .then(res -> {
                        int status = res.getStatus();
                        boolean ok = res.isOk();
                        Assert.isTrue(ok, "更新trackers失败 {} {}", status, url);
                        String contentType = res.header(Header.CONTENT_TYPE);
                        Assert.notBlank(contentType, "更新trackers失败 contentType 为空 {}", url);
                        Assert.isTrue(contentType.contains(ContentType.TEXT_PLAIN.getValue()), "更新trackers失败 {} {}", contentType, url);

                        String body = res.body();
                        StrUtil.split(body, "\n")
                                .stream()
                                .filter(StrUtil::isNotBlank)
                                .map(s -> s.replace("\"", ""))
                                .map(String::trim)
                                .filter(s -> {
                                    for (String string : List.of("udp://", "wss://", "ws://", "https://", "http://")) {
                                        if (s.startsWith(string)) {
                                            return true;
                                        }
                                    }
                                    return false;
                                })
                                .forEach(trackers::add);
                    });
        }

        Assert.isTrue(!trackers.isEmpty(), "获取到0个trackers, 不进行更新");

        String download = config.getDownloadToolType();
        Class<Object> loadClass = ClassUtil.loadClass("ani.rss.download." + download);
        BaseDownload baseDownload = (BaseDownload) ReflectUtil.newInstance(loadClass);
        Boolean login = baseDownload.login(config);
        Assert.isTrue(login, "{} 登录失败", download);
        baseDownload.updateTrackers(trackers);
    }

}
