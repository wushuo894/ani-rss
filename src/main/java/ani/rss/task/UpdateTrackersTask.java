package ani.rss.task;

import ani.rss.download.BaseDownload;
import ani.rss.entity.Config;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class UpdateTrackersTask implements Runnable {
    public static void start() {
        new UpdateTrackersTask().run();
    }

    public static void updateTrackers(Config config) {
        String trackersUpdateUrls = config.getTrackersUpdateUrls();
        Assert.notBlank(trackersUpdateUrls, "Trackers更新地址 为空");

        Set<String> urls = StrUtil.split(trackersUpdateUrls, "\n").stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Assert.isTrue(!urls.isEmpty(), "Trackers更新地址 为空");

        Set<String> trackers = new HashSet<>();

        for (String url : urls) {
            log.info("获取 tracker {}", url);
            HttpReq.get(url, true)
                    .then(res -> {
                        int status = res.getStatus();
                        boolean ok = res.isOk();
                        Assert.isTrue(ok, "更新trackers失败 {} {}", status, url);
                        String contentType = res.header(Header.CONTENT_TYPE);
                        Assert.notBlank(contentType, "更新trackers失败 contentType 为空 {}", url);
                        Assert.isTrue(contentType.contains(ContentType.TEXT_PLAIN.name()), "更新trackers失败 {} {}", contentType, url);

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

        String download = config.getDownload();
        Class<Object> loadClass = ClassUtil.loadClass("ani.rss.download." + download);
        BaseDownload baseDownload = (BaseDownload) ReflectUtil.newInstance(loadClass);
        Boolean login = baseDownload.login(config);
        Assert.isTrue(login, "{} 登录失败", download);
        baseDownload.updateTrackers(trackers);
    }

    @Override
    public void run() {
        CronUtil.schedule("0 1 * * *", new Runnable() {
            @Override
            public void run() {
                Config config = ConfigUtil.CONFIG;
                Boolean autoTrackersUpdate = config.getAutoTrackersUpdate();
                if (!autoTrackersUpdate) {
                    // 未开启自动更新  Trackers
                    return;
                }
                log.info("定时任务 开始更新 Trackers");
                try {
                    UpdateTrackersTask.updateTrackers(config);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        CronUtil.start();
    }

}
