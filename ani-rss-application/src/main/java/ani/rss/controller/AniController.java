package ani.rss.controller;

import ani.rss.annotation.Auth;
import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.commons.PinyinUtils;
import ani.rss.comparator.WeekComparator;
import ani.rss.entity.*;
import ani.rss.entity.dto.IdDTO;
import ani.rss.entity.dto.ImportAniDataDTO;
import ani.rss.entity.dto.RssToAniDTO;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.entity.web.Result;
import ani.rss.enums.AniSortTypeEnum;
import ani.rss.service.AniService;
import ani.rss.service.ClearService;
import ani.rss.service.DownloadService;
import ani.rss.task.RssTask;
import ani.rss.util.other.*;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.*;

@Slf4j
@RestController
public class AniController extends BaseController {
    @Resource
    private AniService aniService;

    @Resource
    private ClearService clearService;

    @Resource
    private DownloadService downloadService;

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
            throw new IllegalArgumentException("此订阅已存在");
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
                throw new IllegalArgumentException("订阅标题重复");
            }
        }

        AniUtil.ANI_LIST.add(ani);
        AniUtil.sync();
        Boolean enable = ani.getEnable();
        if (enable) {
            ThreadUtil.execute(() -> {
                if (TorrentUtil.login()) {
                    downloadService.downloadAni(ani);
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

        // 移动视频文件
        String move = request.getParameter("move");
        if (Boolean.parseBoolean(move)) {
            Ani get = ObjectUtil.clone(first.get());
            ThreadUtil.execute(() -> {
                String downloadPath = downloadService.getDownloadPath(get);
                String newDownloadPath = downloadService.getDownloadPath(ani);
                List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();
                if (downloadPath.equals(newDownloadPath)) {
                    // 位置未发生改变
                    return;
                }

                File downloadPathFile = new File(downloadPath);

                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    String savePath = torrentsInfo.getSavePath();
                    if (!savePath.equals(downloadPath)) {
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
                    clearService.clearDir(downloadPath);
                } catch (Exception e) {
                    log.error(ExceptionUtils.getMessage(e), e);
                }
            });
        }

        //  移动种子
        File torrentDir = TorrentUtil.getTorrentDir(first.get());
        String[] ignoreProperties = new String[]{"currentEpisodeNumber", "lastDownloadTime"};
        BeanUtil.copyProperties(ani, first.get(), ignoreProperties);
        File newTorrentDir = TorrentUtil.getTorrentDir(first.get());
        if (!torrentDir.toString().equals(newTorrentDir.toString())) {
            FileUtil.mkdir(newTorrentDir);
            FileUtil.move(torrentDir, newTorrentDir.getParentFile(), true);
        }
        clearService.clearDir(torrentDir);
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

        AniUtil.ANI_LIST.removeAll(anis);
        AniUtil.sync();

        ThreadUtil.execute(() -> {
            List<TorrentsInfo> torrentsInfos = TorrentUtil.getTorrentsInfos();

            for (Ani ani : anis) {
                File torrentDir = TorrentUtil.getTorrentDir(ani);
                FileUtil.del(torrentDir);
                clearService.clearDir(torrentDir);
                log.info("删除订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());

                if (!deleteFiles) {
                    continue;
                }
                // 删除本地文件
                String downloadPath = downloadService.getDownloadPath(ani);
                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    String savePath = torrentsInfo.getSavePath();
                    if (savePath.equals(downloadPath)) {
                        TorrentUtil.delete(torrentsInfo, true, true);
                    }
                }
                ThreadUtil.sleep(3000);
                clearService.clearDir(downloadPath);
            }
        });
        return Result.success("删除订阅成功");
    }

    @Auth
    @Operation(summary = "订阅列表")
    @PostMapping("/listAni")
    public Result<ListAni> listAni() {
        Config config = ConfigUtil.CONFIG;
        AniSortTypeEnum sortType = config.getSortType();

        ListAni listAni = new ListAni();
        List<ListAni.WeekAni> weekAniList = new ArrayList<>();

        List<String> weeks = List.of("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六");
        Map<String, List<Ani>> weekItemsMap = new HashMap<>();

        for (String week : weeks) {
            List<Ani> items = new ArrayList<>();
            weekAniList.add(new ListAni.WeekAni(week, items));
            weekItemsMap.put(week, items);
        }

        WeekComparator weekComparator = new WeekComparator();
        weekAniList = weekAniList.stream()
                .sorted((a, b) ->
                        weekComparator.compare(a.getWeekLabel(), b.getWeekLabel())
                ).toList();
        listAni.setWeekList(weekAniList);

        // 按拼音排序
        List<Ani> aniList = AniUtil.ANI_LIST;

        List<String> releaseDateList = aniList.stream()
                .map(Ani::getReleaseDate)
                .map(DateUtil::beginOfMonth)
                .sorted(Comparator.comparingLong(Date::getTime).reversed())
                .map(it -> DateUtil.format(it, DatePattern.NORM_MONTH_PATTERN))
                .distinct()
                .toList();
        listAni.setReleaseDateList(releaseDateList)
                .setTotal(aniList.size());

        aniList = CollUtil.sort(aniList, sortType.comparator);

        int index = 0;
        for (Ani ani : aniList) {
            ani.setSort(index++);
            String title = ani.getTitle();
            String pinyin = PinyinUtils.getPinyin(title, "");
            String pinyinInitials = PinyinUtils.getFirstLetter(title, "");

            Date releaseDate = ani.getReleaseDate();
            int week = DateUtil.dayOfWeek(releaseDate) - 1;
            String weekLabel = weeks.get(week);

            ani
                    .setPinyin(pinyin)
                    .setPinyinInitials(pinyinInitials)
                    .setWeekLabel(weekLabel);

            List<Ani> anis = weekItemsMap.get(weekLabel);
            anis.add(ani);
        }

        return Result.success(listAni);
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
                Boolean b = aniService.updateTotalEpisodeNumber(ani, bgmInfo, force);
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
    @Operation(summary = "刷新全部订阅")
    @PostMapping("/refreshAll")
    public Result<Void> refreshAll() {
        RssTask.syncLock();
        // 未传Body, 刷新所有订阅
        ThreadUtil.execute(() -> {
            try {
                RssTask.syncDownload();
            } catch (Exception e) {
                String message = ExceptionUtils.getMessage(e);
                log.error(message, e);
            }
        });
        return Result.success("已开始刷新RSS");
    }

    @Auth
    @Operation(summary = "刷新订阅")
    @PostMapping("/refreshAni")
    public Result<Void> refreshAni(@RequestBody IdDTO dto) {
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(dto.getId()))
                .findFirst();
        if (first.isEmpty()) {
            return Result.error("修改失败");
        }
        Ani downloadAni = first.get();
        RssTask.syncLock();
        ThreadUtil.execute(() -> {
            try {
                RssTask.syncDownload(List.of(downloadAni));
            } catch (Exception e) {
                String message = ExceptionUtils.getMessage(e);
                log.error(message, e);
            }
        });
        return Result.success("已开始刷新RSS {}", downloadAni.getTitle());
    }

    @Auth
    @Operation(summary = "将RSS转换为订阅")
    @PostMapping("/rssToAni")
    public Result<Ani> rssToAni(@RequestBody RssToAniDTO dto) {
        try {
            Ani newAni = AniUtil.getAni(dto);
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

        String downloadPath = downloadService.getDownloadPath(ani);

        for (Item item : items) {
            item.setLocal(false);
            File torrent = TorrentUtil.getTorrent(ani, item);
            if (torrent.exists()) {
                item.setLocal(true);
                continue;
            }
            if (downloadService.itemDownloaded(ani, item, false)) {
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
        String downloadPath = downloadService.getDownloadPath(ani);

        boolean change = false;
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isPresent()) {
            Ani oldAni = ObjectUtil.clone(first.get());
            // 只在名称改变时移动
            oldAni.setSeason(ani.getSeason());
            String oldDownloadPath = downloadService.getDownloadPath(oldAni);
            change = !downloadPath.equals(oldDownloadPath);
        }

        Map<String, Object> map = Map.of(
                "change", change,
                "downloadPath", downloadPath
        );
        return Result.success(map);
    }

    @Auth
    @Operation(summary = "导入订阅")
    @PostMapping("/importAni")
    public Result<Void> importAni(@RequestBody ImportAniDataDTO dto) {
        List<Ani> aniList = dto.getAniList();
        if (aniList.isEmpty()) {
            return Result.error("导入列表为空");
        }

        ImportAniDataDTO.Conflict conflict = dto.getConflict();

        for (Ani ani : aniList) {
            AniUtil.verify(ani);

            String title = ani.getTitle();
            int season = ani.getSeason();
            Optional<Ani> first = AniUtil.ANI_LIST.stream()
                    .filter(it -> it.getTitle().equals(title) && it.getSeason() == season)
                    .findFirst();

            if (first.isEmpty()) {
                String image = ani.getImage();
                String cover = AniUtil.saveCover(image);
                ani.setCover(cover)
                        .setId(UUID.fastUUID().toString());
                AniUtil.ANI_LIST.add(ani);
                continue;
            }

            if (conflict == ImportAniDataDTO.Conflict.SKIP) {
                log.info("存在冲突，已跳过 {} 第{}季", title, season);
                continue;
            }

            log.info("存在冲突，已替换 {} 第{}季", title, season);
            String image = ani.getImage();
            String cover = AniUtil.saveCover(image);
            ani.setCover(cover);

            String[] ignoreProperties = new String[]{"id", "currentEpisodeNumber", "lastDownloadTime"};
            BeanUtil.copyProperties(ani, first.get(), ignoreProperties);
        }

        AniUtil.sync();
        return Result.success("导入成功");
    }

    @Auth
    @Operation(summary = "刷新封面")
    @PostMapping("/refreshCover")
    public Result<String> refreshCover(@RequestBody Ani ani) {
        String s = AniUtil.saveCover(ani.getImage(), true);
        return Result.success(r -> r.setData(s));
    }

}
