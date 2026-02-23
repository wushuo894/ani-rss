package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.entity.*;
import ani.rss.enums.SortTypeEnum;
import ani.rss.service.AniService;
import ani.rss.service.ClearService;
import ani.rss.service.DownloadService;
import ani.rss.task.RssTask;
import ani.rss.util.other.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.PinyinComparator;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.ToLongFunction;

@Slf4j
@RestController
public class AniController {
    public static final AtomicBoolean DOWNLOAD = new AtomicBoolean(false);

    @Auth
    @Operation(summary = "添加订阅")
    @PostMapping("/addAni")
    public Result<Void> addAni(@RequestBody Ani ani) {
        ani.setTitle(ani.getTitle().trim())
                .setUrl(ani.getUrl().trim());
        AniUtil.verify(ani);

        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();

        if (first.isPresent()) {
            return Result.error("此订阅已存在");
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();

        String title = ani.getTitle();
        Integer season = ani.getSeason();

        if (first.isPresent()) {
            Config config = ConfigUtil.CONFIG;
            Boolean replace = config.getReplace();
            if (replace) {
                AniUtil.ANI_LIST.remove(first.get());
                log.info("自动替换 {} 第{}季", title, season);
            } else {
                return Result.error("订阅标题重复");
            }
        }

        AniUtil.ANI_LIST.add(ani);
        AniUtil.sync();
        Boolean enable = ani.getEnable();
        if (enable) {
            ThreadUtil.execute(() -> {
                if (TorrentUtil.login()) {
                    DownloadService.downloadAni(ani);
                }
            });
        } else {
            // 如果未开启订阅则只获取一下集数
            ThreadUtil.execute(() -> {
                try {
                    List<Item> items = ItemsUtil.getItems(ani);
                    int currentEpisodeNumber = ItemsUtil.currentEpisodeNumber(ani, items);
                    ani.setCurrentEpisodeNumber(currentEpisodeNumber);
                } catch (Exception e) {
                    log.error(ExceptionUtils.getMessage(e), e);
                }
            });
        }
        log.info("添加订阅 {} {} {}", title, ani.getUrl(), ani.getId());

        return Result.success("添加订阅成功");
    }

    @Auth
    @Operation(summary = "修改订阅")
    @PostMapping("/setAni")
    public Result<Void> setAni(@RequestBody Ani ani) {
        ani.setTitle(ani.getTitle().trim())
                .setUrl(ani.getUrl().trim());
        AniUtil.verify(ani);
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> !it.getId().equals(ani.getId()))
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();
        if (first.isPresent()) {
            return Result.error("订阅标题重复");
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isEmpty()) {
            return Result.error("修改失败");
        }
        HttpServletRequest request = Global.REQUEST.get();
        String move = request.getParameter("move");
        if (Boolean.parseBoolean(move)) {
            Ani get = ObjectUtil.clone(first.get());
            ThreadUtil.execute(() -> {
                String downloadPath = DownloadService.getDownloadPath(get);
                String newDownloadPath = DownloadService.getDownloadPath(ani);
                Boolean login = TorrentUtil.login();
                List<TorrentsInfo> torrentsInfos = new ArrayList<>();
                if (login) {
                    torrentsInfos = TorrentUtil.getTorrentsInfos();
                }
                if (downloadPath.equals(newDownloadPath)) {
                    // 位置未发生改变
                    return;
                }

                File downloadPathFile = new File(downloadPath);

                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    String downloadDir = torrentsInfo.getDownloadDir();
                    if (!downloadDir.equals(downloadPath)) {
                        // 旧位置不相同
                        continue;
                    }
                    // 修改保存位置
                    TorrentUtil.setSavePath(torrentsInfo, newDownloadPath);
                }
                if (!downloadPathFile.exists()) {
                    return;
                }
                if (downloadPathFile.isFile()) {
                    return;
                }
                if (!torrentsInfos.isEmpty()) {
                    ThreadUtil.sleep(3000);
                }
                try {
                    FileUtil.mkdir(newDownloadPath);
                    File[] files = FileUtils.listFiles(downloadPath);
                    for (File oldFile : files) {
                        log.info("移动文件 {} ==> {}", oldFile, newDownloadPath);
                        FileUtil.move(oldFile, new File(newDownloadPath), true);
                    }
                    FileUtil.del(downloadPath);
                    ClearService.clearParentFile(downloadPath);
                } catch (Exception e) {
                    log.error(ExceptionUtils.getMessage(e), e);
                }
            });
        }
        File torrentDir = TorrentUtil.getTorrentDir(first.get());

        String[] ignoreProperties = new String[]{"currentEpisodeNumber", "lastDownloadTime"};
        BeanUtil.copyProperties(ani, first.get(), ignoreProperties);

        File newTorrentDir = TorrentUtil.getTorrentDir(first.get());
        if (!torrentDir.toString().equals(newTorrentDir.toString())) {
            FileUtil.move(torrentDir, newTorrentDir.getParentFile(), true);
        }
        AniUtil.sync();

        log.info("修改订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());
        return Result.success("修改成功");
    }

    @Auth
    @Operation(summary = "删除订阅")
    @PostMapping("/deleteAni")
    public Result<Void> deleteAni(@RequestBody List<String> ids, @RequestParam("deleteFiles") Boolean deleteFiles) {
        Assert.notEmpty(ids, "未选择订阅");
        List<Ani> anis = AniUtil.ANI_LIST.stream()
                .filter(it -> ids.contains(it.getId()))
                .toList();
        if (anis.isEmpty()) {
            return Result.error("删除失败");
        }
        for (Ani ani : anis) {
            AniUtil.ANI_LIST.remove(ani);
        }

        AniUtil.sync();
        ThreadUtil.execute(() -> {
            for (Ani ani : anis) {
                File torrentDir = TorrentUtil.getTorrentDir(ani);
                FileUtil.del(torrentDir);
                ClearService.clearParentFile(torrentDir);
                log.info("删除订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());
            }
            if (!deleteFiles) {
                // 不删除本地文件
                return;
            }

            List<File> files = anis
                    .stream()
                    .map(DownloadService::getDownloadPath)
                    .map(File::new)
                    .toList();

            Boolean login = TorrentUtil.login();
            List<TorrentsInfo> torrentsInfos = new ArrayList<>();
            if (login) {
                torrentsInfos = TorrentUtil.getTorrentsInfos();
            }
            for (File file : files) {
                String path = FileUtils.getAbsolutePath(file);
                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    String downloadDir = torrentsInfo.getDownloadDir();
                    if (downloadDir.equals(path)) {
                        TorrentUtil.delete(torrentsInfo, true, true);
                    }
                }
                if (!file.exists()) {
                    continue;
                }
                ThreadUtil.sleep(3000);
                log.info("删除 {}", file);
                FileUtil.del(file);
                ClearService.clearParentFile(file);
            }
        });
        return Result.success("删除订阅成功");
    }

    @Auth
    @Operation(summary = "订阅列表")
    @PostMapping("/listAni")
    public Result<List<Ani>> list() {
        Config config = ConfigUtil.CONFIG;

        SortTypeEnum sortType = config.getSortType();

        // 按拼音排序
        List<Ani> list = AniUtil.ANI_LIST;

        list
                .parallelStream()
                .forEach(ani -> {
                    String title = ani.getTitle();
                    String pinyin = PinyinUtil.getPinyin(title, "");
                    String pinyinInitials = PinyinUtil.getFirstLetter(title, "");

                    Integer year = ani.getYear();
                    Integer month = ani.getMonth();
                    Integer date = ani.getDate();

                    String format = StrFormatter.format("{}-{}-{}", year, month, date);

                    int week = 0;
                    try {
                        DateTime dateTime = DateUtil.parse(format, DatePattern.NORM_DATE_PATTERN);
                        week = DateUtil.dayOfWeek(dateTime) - 1;
                    } catch (Exception e) {
                        log.error("日期存在问题 {} {}", title, format);
                    }

                    ani.setPinyin(pinyin)
                            .setPinyinInitials(pinyinInitials)
                            .setWeek(week);
                });

        if (sortType == SortTypeEnum.SCORE) {
            list = CollUtil.sort(list, Comparator.comparingDouble(Ani::getScore).reversed());
        }

        if (sortType == SortTypeEnum.PINYIN) {
            PinyinComparator pinyinComparator = new PinyinComparator();
            list = CollUtil.sort(list, (a, b) -> pinyinComparator.compare(a.getTitle(), b.getTitle()));
        }

        if (sortType == SortTypeEnum.DOWNLOAD_TIME) {
            list = CollUtil.sort(list, Comparator.comparingLong((ToLongFunction<Ani>) ani -> {
                Long lastDownloadTime = ani.getLastDownloadTime();
                if (lastDownloadTime == 0) {
                    return Long.MAX_VALUE;
                }
                return lastDownloadTime;
            }).reversed());
        }

        return Result.success(list);
    }

    @Auth
    @Operation(summary = "更新总集数")
    @PostMapping("/updateTotalEpisodeNumber")
    public Result<Void> updateTotalEpisodeNumber(@RequestParam("force") Boolean force, @RequestBody List<String> ids) {
        Assert.notEmpty(ids, "未选择订阅");
        ThreadUtil.execute(() -> {
            log.info("开始手动更新总集数");
            int count = 0;
            for (Ani ani : AniUtil.ANI_LIST) {
                String id = ani.getId();
                if (!ids.contains(id)) {
                    continue;
                }
                BgmInfo bgmInfo;
                try {
                    bgmInfo = BgmUtil.getBgmInfo(ani);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    continue;
                }
                Boolean b = AniService.updateTotalEpisodeNumber(ani, bgmInfo, force);
                if (b) {
                    count++;
                }
            }
            AniUtil.sync();
            log.info("手动更新总集数完成 共更新{}条订阅", count);
        });
        return Result.success("已开始更新总集数");
    }

    @Auth
    @Operation(summary = "批量 启用/禁用 订阅")
    @PostMapping("/batchEnable")
    public Result<Void> batchEnable(@RequestParam("value") Boolean value, @RequestBody List<String> ids) {
        Assert.notEmpty(ids, "未选择订阅");
        for (Ani ani : AniUtil.ANI_LIST) {
            String id = ani.getId();
            if (!ids.contains(id)) {
                continue;
            }
            ani.setEnable(value);
        }
        AniUtil.sync();
        return Result.success("修改完成");
    }

    @Auth
    @Operation(summary = "手动刷新订阅")
    @PostMapping("/refreshAll")
    public Result<Void> refreshAni() {
        // 未传Body, 刷新所有订阅
        RssTask.sync();
        ThreadUtil.execute(() -> RssTask.download(new AtomicBoolean(true)));
        return Result.success("已开始刷新RSS");
    }

    @Auth
    @Operation(summary = "手动刷新订阅")
    @PostMapping("/refreshAni")
    public Result<Void> refreshAni(@RequestBody Ani ani) {
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isEmpty()) {
            return Result.error("修改失败");
        }
        synchronized (DOWNLOAD) {
            if (DOWNLOAD.get()) {
                return Result.error("存在未完成任务，请等待...");
            }
            DOWNLOAD.set(true);
        }
        Ani downloadAni = first.get();
        ThreadUtil.execute(() -> {
            try {
                if (TorrentUtil.login()) {
                    DownloadService.downloadAni(downloadAni);
                }
            } catch (Exception e) {
                String message = ExceptionUtils.getMessage(e);
                log.error(message, e);
            }
            DOWNLOAD.set(false);
        });
        return Result.success("已开始刷新RSS {}", downloadAni.getTitle());
    }

    @Auth
    @Operation(summary = "将RSS转换为订阅")
    @PostMapping("/rssToAni")
    public Result<Ani> rssToAni(@RequestBody Ani ani) {
        String url = ani.getUrl();
        String type = ani.getType();
        String bgmUrl = ani.getBgmUrl();
        Assert.notBlank(url, "RSS地址 不能为空");
        if (!ReUtil.contains("http(s*)://", url)) {
            url = "https://" + url;
        }
        url = URLUtil.decode(url, "utf-8");
        try {
            Ani newAni = AniUtil.getAni(url, type, bgmUrl);
            return Result.success(newAni);
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(message, e);
            return Result.error("RSS解析失败 {}", message);
        }
    }

    @Auth
    @Operation(summary = "预览订阅")
    @PostMapping("/previewAni")
    public Result<Map<String, Object>> previewAni(@RequestBody Ani ani) {
        List<Item> items = ItemsUtil.getItems(ani);

        String downloadPath = DownloadService.getDownloadPath(ani);

        for (Item item : items) {
            item.setLocal(false);
            File torrent = TorrentUtil.getTorrent(ani, item);
            if (torrent.exists()) {
                item.setLocal(true);
                continue;
            }
            if (DownloadService.itemDownloaded(ani, item, false)) {
                item.setLocal(true);
            }
        }

        List<Integer> omitList = ItemsUtil.omitList(ani, items);

        Map<String, Object> map = Map.of(
                "downloadPath", downloadPath,
                "items", items,
                "omitList", omitList
        );
        return Result.success(map);
    }

    @Auth
    @Operation(summary = "获取订阅的下载位置")
    @PostMapping("/downloadPath")
    public Result<Map<String, Object>> downloadPath(@RequestBody Ani ani) {
        String downloadPath = DownloadService.getDownloadPath(ani);

        boolean change = false;
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isPresent()) {
            Ani oldAni = ObjectUtil.clone(first.get());
            // 只在名称改变时移动
            oldAni.setSeason(ani.getSeason());
            String oldDownloadPath = DownloadService.getDownloadPath(oldAni);
            change = !downloadPath.equals(oldDownloadPath);
        }

        Map<String, Object> map = Map.of(
                "change", change,
                "downloadPath", downloadPath
        );
        return Result.success(map);
    }
}
