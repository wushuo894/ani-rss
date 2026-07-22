package ani.rss.download;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.config.OpenListConfig;
import ani.rss.entity.*;
import ani.rss.entity.torrent.TorrentsInfo;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.other.ConfigUtil;
import ani.rss.util.other.NotificationUtil;
import ani.rss.util.other.OpenListUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenList implements BaseDownload {
    private static final Config CONFIG = ConfigUtil.CONFIG;

    private final OpenListUtil openListUtil = OpenListUtil.getInstance(new OpenListConfig() {
        @Override
        public String getServer() {
            return CONFIG.getDownloadToolHost();
        }

        @Override
        public String getApiKey() {
            return CONFIG.getDownloadToolPassword();
        }
    });

    @Override
    public Boolean login(Boolean test, Config config) {
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        if (StrUtil.isBlank(host) || StrUtil.isBlank(password)) {
            log.warn("OpenList 未配置完成");
            return false;
        }
        String downloadPath = config.getDownloadPathTemplate();
        Assert.notBlank(downloadPath, "未设置下载位置");
        String provider = config.getProvider();
        Assert.notBlank(provider, "请选择 Driver");
        try {
            return OpenListUtil.getInstance(host, password).test();
        } catch (Exception e) {
            String message = ExceptionUtils.getMessage(e);
            log.error(e.getMessage(), e);
            log.error("登录 OpenList 失败 {}", message);
        }
        return false;
    }


    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        return List.of();
    }

    @Override
    public Boolean download(Ani ani, Item item, String savePath, File torrentFile) {
        // windows 真该死啊
        savePath = ReUtil.replaceAll(savePath, "^[A-z]:", "");

        String magnet = TorrentUtil.getMagnet(torrentFile);
        String reName = item.getReName();
        String path = savePath + "/" + reName;
        Boolean standbyRss = CONFIG.getStandbyRss();
        Boolean delete = CONFIG.getDelete();
        Boolean coexist = CONFIG.getCoexist();
        try {
            openListUtil.mkdir(path);

            // 删除残留任务
            openListUtil.deleteResidualTasks(magnet);

            // 洗版，删除备 用RSS 所下载的视频
            if (standbyRss && delete && !coexist) {
                String s = ReUtil.get(StringEnum.SEASON_REG, reName, 0);
                String finalSavePath = savePath;
                openListUtil.fsList(savePath, true)
                        .stream()
                        .map(OpenListFileInfo::getName)
                        .filter(name -> name.contains(s))
                        .forEach(name -> {
                            openListUtil.fsRemove(finalSavePath, List.of(name));
                            log.info("已开启备用RSS, 自动删除 {}/{}", finalSavePath, name);
                        });
            }
            String tid;
            try {
                tid = openListUtil.fsAddOfflineDownload(magnet, path, CONFIG.getProvider());
                log.info("添加离线下载成功 {}", reName);
            } catch (Exception e) {
                log.error("添加离线下载失败 {}", reName);
                throw new IllegalStateException("添加离线下载失败 " + reName);
            }

            // 记录开始时间
            DateTime startTime = DateTime.now();

            // 重试次数
            long retry = 0;
            while (true) {
                Integer openListDownloadTimeout = CONFIG.getOpenListDownloadTimeout();
                Long openListDownloadRetryNumber = CONFIG.getOpenListDownloadRetryNumber();

                DateTime endTime = DateUtil.offsetMinute(startTime, openListDownloadTimeout);
                DateTime currentTime = DateTime.now();
                if (currentTime.getTime() >= endTime.getTime()) {
                    // 超过下载超时限制
                    log.error("{} {} 分钟还未下载完成, 停止检测下载", reName, openListDownloadTimeout);
                    return false;
                }

                Optional<OpenListTaskInfo> taskInfoOpt = openListUtil.taskInfo(tid);

                if (taskInfoOpt.isEmpty()) {
                    continue;
                }

                OpenListTaskInfo taskInfo = taskInfoOpt.get();
                OpenListTaskInfo.State state = taskInfo.getState();
                String error = taskInfo.getError();

                // errored 重试
                if (
                        List.of(
                                OpenListTaskInfo.State.Error,
                                OpenListTaskInfo.State.Failing,
                                OpenListTaskInfo.State.Failed
                        ).contains(state)
                ) {
                    // 已到达最大重试次数 5 次, -1 不限制
                    if (openListDownloadRetryNumber > -1) {
                        if (retry >= openListDownloadRetryNumber) {
                            // bug fix: 新资源下载完成后，OpenList 状态可能未及时刷新
                            // 此处通过检查文件是否存在来兜底，存在则直接继续后续逻辑
                            Optional<OpenListFileInfo> first = openListUtil.findFiles(path)
                                    .stream()
                                    .filter(openListFileInfo -> FileUtils.isVideoFormat(openListFileInfo.getName()))
                                    .findFirst();
                            if (first.isPresent()) {
                                log.info("资源已下载完毕，OpenList 可能处于卡死状态，此处跳过");
                                break;
                            }
                            log.error("离线下载失败 {}", error);
                            return false;
                        }
                        retry++;
                        log.info("离线任务正在进行重试 {}, 当前重试次数 {}, 最大重试次数 {}", tid, retry, openListDownloadRetryNumber);
                    }
                    openListUtil.taskRetry(tid);
                    continue;
                }

                if (
                        List.of(
                                OpenListTaskInfo.State.Canceling,
                                OpenListTaskInfo.State.Canceled
                        ).contains(state)
                ) {
                    log.error("离线任务已被取消 {}", reName);
                    return false;
                }

                // 成功
                if (state == OpenListTaskInfo.State.Succeeded) {
                    break;
                }
            }

            if (delete) {
                log.info("离线下载完成, 自动删除已完成任务");
                openListUtil.taskDelete(tid);
            }

            List<OpenListFileInfo> openListFileInfos = openListUtil.findFiles(path);

            // 取大小最大的一个视频文件
            Optional<OpenListFileInfo> videoFileOpt = openListFileInfos.stream()
                    .filter(openListFileInfo ->
                            FileUtils.isVideoFormat(openListFileInfo.getName()))
                    .findFirst();

            if (videoFileOpt.isEmpty()) {
                return false;
            }
            OpenListFileInfo videoFile = videoFileOpt.get();
            List<OpenListFileInfo> subtitleList = openListFileInfos.stream()
                    .filter(openListFileInfo ->
                            FileUtils.isSubtitleFormat(openListFileInfo.getName()))
                    .toList();

            Map<String, String> renameMap = new HashMap<>();
            renameMap.put(videoFile.getName(), reName + "." + FileUtil.extName(videoFile.getName()));
            for (OpenListFileInfo openListFileInfo : subtitleList) {
                String name = openListFileInfo.getName();
                String extName = FileUtil.extName(name);
                String newName = reName;
                String lang = FileUtil.extName(FileUtil.mainName(name));
                if (StrUtil.isNotBlank(lang)) {
                    newName = newName + "." + lang;
                }
                renameMap.put(name, newName + "." + extName);
            }

            Boolean rename = CONFIG.getRename();

            if (rename) {
                // 重命名
                List<Map<String, String>> renameObjects = renameMap.entrySet().stream()
                        .map(map -> {
                            String srcName = map.getKey();
                            String newName = map.getValue();
                            log.info("重命名 {} ==> {}", srcName, newName);
                            return Map.of(
                                    "src_name", srcName,
                                    "new_name", newName
                            );
                        }).toList();
                openListUtil.fsBatchRename(renameObjects, videoFile.getPath());
            }

            // 移动
            List<String> names = renameMap.entrySet()
                    .stream()
                    .map(m -> rename ? m.getValue() : m.getKey())
                    .toList();
            openListUtil.fsMove(videoFile.getPath(), savePath, names);

            // 删除残留文件夹
            openListUtil.fsRemove(savePath, List.of(reName));

            NotificationUtil.send(CONFIG, ani,
                    StrFormatter.format("{} 下载完成", item.getReName()),
                    NotificationStatusEnum.DOWNLOAD_END
            );
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Boolean delete(TorrentsInfo torrentsInfo, Boolean deleteFiles) {
        return false;
    }

    @Override
    public Boolean rename(TorrentsInfo torrentsInfo) {
        return false;
    }

    @Override
    public Boolean addTags(TorrentsInfo torrentsInfo, String tags) {
        return false;
    }

    @Override
    public void updateTrackers(Set<String> trackers) {

    }

    @Override
    public void setSavePath(TorrentsInfo torrentsInfo, String path) {

    }


}
