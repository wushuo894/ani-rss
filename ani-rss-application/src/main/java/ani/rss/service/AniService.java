package ani.rss.service;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.commons.PinyinUtils;
import ani.rss.comparator.WeekComparator;
import ani.rss.entity.*;
import ani.rss.entity.dto.IdDTO;
import ani.rss.entity.dto.ImportAniDataDTO;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.enums.AniSortTypeEnum;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Slf4j
@Service
public class AniService {
    @Resource
    private DownloadService downloadService;

    @Resource
    private ClearService clearService;

    /**
     * 添加订阅
     *
     * @param ani 订阅
     */
    public void addAni(Ani ani) {
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
    }

    /**
     * 修改订阅
     *
     * @param ani 订阅
     */
    public void setAni(Ani ani) {
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> !it.getId().equals(ani.getId()))
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();
        if (first.isPresent()) {
            throw new IllegalArgumentException("订阅标题重复");
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(ani.getId()))
                .findFirst();
        if (first.isEmpty()) {
            throw new IllegalArgumentException("订阅不存在");
        }
        HttpServletRequest request = Global.REQUEST.get();

        // 移动视频文件
        String move = request.getParameter("move");
        if (Boolean.parseBoolean(move)) {
            Ani oldAni = ObjectUtil.clone(first.get());
            ThreadUtil.execute(() -> {
                String downloadPath = downloadService.getDownloadPath(oldAni);
                String newDownloadPath = downloadService.getDownloadPath(ani);
                if (downloadPath.equals(newDownloadPath)) {
                    // 位置未发生改变
                    return;
                }

                List<TorrentsInfo> torrentsInfos = TorrentUtil.findTorrentsInfosByAni(oldAni);
                for (TorrentsInfo torrentsInfo : torrentsInfos) {
                    // 修改保存位置
                    TorrentUtil.setSavePath(torrentsInfo, newDownloadPath);
                }

                try {
                    FileUtil.mkdir(newDownloadPath);
                    List<File> files = FileUtils.listFileList(downloadPath);
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
        if (!torrentDir.equals(newTorrentDir)) {
            FileUtil.mkdir(newTorrentDir);
            if (torrentDir.exists()) {
                FileUtil.move(torrentDir, newTorrentDir.getParentFile(), true);
            }
        }
        clearService.clearDir(torrentDir);
        AniUtil.sync();

        log.info("修改订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());
    }

    public void deleteAni(List<String> ids, Boolean deleteFiles) {
        Assert.notEmpty(ids, "未选择订阅");
        List<Ani> anis = AniUtil.ANI_LIST.stream()
                .filter(it -> ids.contains(it.getId()))
                .toList();
        if (anis.isEmpty()) {
            throw new IllegalArgumentException("订阅不存在");
        }

        AniUtil.ANI_LIST.removeAll(anis);
        AniUtil.sync();

        ThreadUtil.execute(() -> {
            // 删除种子
            for (Ani ani : anis) {
                File torrentDir = TorrentUtil.getTorrentDir(ani);
                FileUtil.del(torrentDir);
                clearService.clearDir(torrentDir);
                log.info("删除订阅 {} {} {}", ani.getTitle(), ani.getUrl(), ani.getId());
            }

            if (deleteFiles) {
                // 删除任务
                List<TorrentsInfo> torrentsInfoList = TorrentUtil.findTorrentsInfosByAni(anis);
                for (TorrentsInfo torrentsInfo : torrentsInfoList) {
                    TorrentUtil.delete(torrentsInfo, true, true);
                }
                // 删除本地视频
                for (Ani ani : anis) {
                    String downloadPath = downloadService.getDownloadPath(ani);
                    FileUtil.del(downloadPath);
                    log.info("删除本地文件 {}", downloadPath);
                    clearService.clearDir(downloadPath);
                }
            }
        });
    }

    public ListAni listAni() {
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
        return listAni;
    }

    public void updateTotalEpisodeNumber(Boolean force, List<String> ids) {
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
                Boolean b = updateTotalEpisodeNumber(ani, bgmInfo, force);
                if (b) {
                    count++;
                }
            }
            AniUtil.sync();
            log.info("手动更新总集数完成 共更新{}条订阅", count);
        });
    }

    /**
     * 更新订阅总集数
     *
     * @param ani     订阅
     * @param bgmInfo bgm信息
     * @param force   强制
     * @return 是否已发生更新
     */
    public Boolean updateTotalEpisodeNumber(Ani ani, BgmInfo bgmInfo, Boolean force) {
        Integer totalEpisodeNumber = ani.getTotalEpisodeNumber();
        if (!force) {
            // 未开启强制更新
            if (totalEpisodeNumber > 0) {
                // 总集数不为 0
                return false;
            }
        }

        String title = ani.getTitle();

        // 自动更新总集数信息
        int bgmEp = BgmUtil.getEps(bgmInfo);
        if (bgmEp == totalEpisodeNumber) {
            // 集数未发生改变
            return false;
        }

        ani.setTotalEpisodeNumber(bgmEp);

        log.info("{} 总集数发生更新: {}", title, bgmEp);
        return true;
    }

    public void batchEnable(Boolean value, List<String> ids) {
        Assert.notEmpty(ids, "未选择订阅");

        for (Ani ani : AniUtil.ANI_LIST) {
            String id = ani.getId();
            if (!ids.contains(id)) {
                continue;
            }
            ani.setEnable(value);
        }
        AniUtil.sync();
    }

    public void refreshAll() {
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
    }

    public void refreshAni(IdDTO dto) {
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getId().equals(dto.getId()))
                .findFirst();
        if (first.isEmpty()) {
            throw new IllegalArgumentException("订阅不存在");
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
    }

    public Map<String, Object> previewAni(Ani ani) {
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

        return Map.of(
                "downloadPath", downloadPath,
                "items", items,
                "omitList", omitList
        );
    }

    public Map<String, Object> downloadPath(Ani ani) {
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

        return Map.of(
                "change", change,
                "downloadPath", downloadPath
        );
    }

    public void importAni(ImportAniDataDTO dto) {
        List<Ani> aniList = dto.getAniList();
        if (aniList.isEmpty()) {
            throw new IllegalArgumentException("导入列表为空");
        }

        ImportAniDataDTO.Conflict conflict = dto.getConflict();

        for (Ani ani : aniList) {
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
    }
}
