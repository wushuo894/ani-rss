package ani.rss.download;

import ani.rss.commons.ExceptionUtils;
import ani.rss.commons.FileUtils;
import ani.rss.commons.GsonStatic;
import ani.rss.entity.*;
import ani.rss.entity.web.Header;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.basic.HttpReq;
import ani.rss.util.other.NotificationUtil;
import ani.rss.util.other.TorrentUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenList implements BaseDownload {
    private Config config;

    @Override
    public Boolean login(Boolean test, Config config) {
        this.config = config;
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
            return postApi("me")
                    .setMethod(Method.GET)
                    .thenFunction(res -> {
                        if (!res.isOk()) {
                            log.error("登录 OpenList 失败");
                            return false;
                        }
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        if (jsonObject.get("code").getAsInt() != 200) {
                            log.error("登录 OpenList 失败");
                            return false;
                        }
                        return true;
                    });
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
        Boolean standbyRss = config.getStandbyRss();
        Boolean delete = config.getDelete();
        Boolean coexist = config.getCoexist();
        try {
            mkdir(path);

            // 删除残留任务
            deleteResidualTasks(magnet);

            // 洗版，删除备 用RSS 所下载的视频
            if (standbyRss && delete && !coexist) {
                String s = ReUtil.get(StringEnum.SEASON_REG, reName, 0);
                String finalSavePath = savePath;
                fsList(savePath, true)
                        .stream()
                        .map(OpenListFileInfo::getName)
                        .filter(name -> name.contains(s))
                        .forEach(name -> {
                            fsRemove(finalSavePath, List.of(name));
                            log.info("已开启备用RSS, 自动删除 {}/{}", finalSavePath, name);
                        });
            }
            String tid;
            try {
                tid = fsAddOfflineDownload(magnet, path);
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
                Integer openListDownloadTimeout = config.getOpenListDownloadTimeout();
                Long openListDownloadRetryNumber = config.getOpenListDownloadRetryNumber();

                DateTime endTime = DateUtil.offsetMinute(startTime, openListDownloadTimeout);
                DateTime currentTime = DateTime.now();
                if (currentTime.getTime() >= endTime.getTime()) {
                    // 超过下载超时限制
                    log.error("{} {} 分钟还未下载完成, 停止检测下载", reName, openListDownloadTimeout);
                    return false;
                }

                Optional<OpenListTaskInfo> taskInfoOpt = taskInfo(tid);

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
                            Optional<OpenListFileInfo> first = findFiles(path).stream()
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
                    taskRetry(tid);
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
                taskDelete(tid);
            }

            List<OpenListFileInfo> openListFileInfos = findFiles(path);

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

            Boolean rename = config.getRename();

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
                fsBatchRename(renameObjects, videoFile.getPath());
            }

            // 移动
            List<String> names = renameMap.entrySet()
                    .stream()
                    .map(m -> rename ? m.getValue() : m.getKey())
                    .toList();
            fsMove(videoFile.getPath(), savePath, names);

            // 删除残留文件夹
            fsRemove(savePath, List.of(reName));

            NotificationUtil.send(config, ani,
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

    /**
     * 创建文件夹
     *
     * @param path 路径
     */
    public void mkdir(String path) {
        postApi("fs/mkdir")
                .body(GsonStatic.toJson(Map.of(
                        "path", path
                )))
                .then(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    int code = jsonObject.get("code").getAsInt();
                    String message = jsonObject.get("message").getAsString();
                    if (code == 200) {
                        log.info("创建文件夹: {}", path);
                        return;
                    }

                    if (!message.startsWith("failed to check if dir exists")) {
                        return;
                    }

                    Path pathObj = Path.of(path);

                    if (pathObj.getNameCount() <= 1) {
                        return;
                    }

                    String parentPath = pathObj
                            .getParent()
                            .toString()
                            .replace('\\', '/');
                    mkdir(parentPath);
                    mkdir(path);
                });
    }

    /**
     * 移动文件
     *
     * @param srcDir 原目录
     * @param dstDir 目标目录
     * @param names  文件名
     */
    public void fsMove(String srcDir, String dstDir, List<String> names) {
        postApi("fs/move")
                .body(GsonStatic.toJson(Map.of(
                        "src_dir", srcDir,
                        "dst_dir", dstDir,
                        "names", names
                ))).then(res -> log.info(res.body()));
    }

    /**
     * 删除文件
     *
     * @param dir   目录
     * @param names 文件名
     */
    public void fsRemove(String dir, List<String> names) {
        postApi("fs/remove")
                .body(GsonStatic.toJson(Map.of(
                        "dir", dir,
                        "names", names
                ))).then(HttpResponse::isOk);
    }

    /**
     * 批量重命名
     *
     * @param mapList 重命名列表
     * @param srcDir  目录
     */
    public void fsBatchRename(List<Map<String, String>> mapList, String srcDir) {
        postApi("fs/batch_rename")
                .body(GsonStatic.toJson(Map.of(
                        "src_dir", srcDir,
                        "rename_objects", mapList
                ))).then(res -> log.info(res.body()));
    }

    /**
     * 添加离线下载
     *
     * @param magnet 磁力链接
     * @param path   离线位置
     * @return tid
     */
    public String fsAddOfflineDownload(String magnet, String path) {
        return postApi("fs/add_offline_download")
                .body(GsonStatic.toJson(Map.of(
                        "path", path,
                        "urls", List.of(magnet),
                        "tool", config.getProvider(),
                        "delete_policy", "delete_on_upload_succeed"
                )))
                .thenFunction(res -> {
                    HttpReq.assertStatus(res);
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    log.debug(jsonObject.toString());
                    Assert.isTrue(jsonObject.get("code").getAsInt() == 200);
                    return jsonObject.getAsJsonObject("data")
                            .getAsJsonArray("tasks")
                            .get(0).getAsJsonObject()
                            .get("id").getAsString();
                });
    }

    /**
     * 文件列表
     *
     * @param path 目录
     * @return 文件列表
     */
    public List<OpenListFileInfo> fsList(String path, Boolean refresh) {
        try {
            return postApi("fs/list")
                    .body(GsonStatic.toJson(Map.of(
                            "path", path,
                            "page", 1,
                            "per_page", 0,
                            "refresh", refresh
                    )))
                    .thenFunction(res -> {
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        int code = jsonObject.get("code").getAsInt();
                        if (code != 200) {
                            return List.of();
                        }
                        JsonElement data = jsonObject.get("data");
                        if (Objects.isNull(data) || data.isJsonNull()) {
                            return List.of();
                        }
                        JsonElement content = data.getAsJsonObject()
                                .get("content");
                        if (Objects.isNull(content) || content.isJsonNull()) {
                            return List.of();
                        }
                        List<OpenListFileInfo> infos = GsonStatic.fromJsonList(content.getAsJsonArray(), OpenListFileInfo.class);
                        for (OpenListFileInfo info : infos) {
                            info.setPath(path);
                        }
                        return ListUtil.sort(new ArrayList<>(infos), Comparator.comparing(fileInfo -> {
                            Long size = fileInfo.getSize();
                            return Long.MAX_VALUE - ObjectUtil.defaultIfNull(size, 0L);
                        }));
                    });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return List.of();
    }

    /**
     * 查看任务
     *
     * @param tid 任务id
     * @return 任务信息
     */
    public Optional<OpenListTaskInfo> taskInfo(String tid) {
        try {
            OpenListTaskInfo taskInfo = postApi("task/offline_download/info?tid=" + tid)
                    .thenFunction(res -> {
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        JsonObject data = jsonObject.get("data").getAsJsonObject();
                        return GsonStatic.fromJson(data, OpenListTaskInfo.class);
                    });
            return Optional.of(taskInfo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * 删除残留任务
     *
     * @param magnet 磁力
     */
    public void deleteResidualTasks(String magnet) {
        List<OpenListTaskInfo> taskDoneList = taskDoneList();
        List<OpenListTaskInfo> taskUnDoneList = taskUnDoneList();

        List<OpenListTaskInfo> tasks = new ArrayList<>();
        tasks.addAll(taskDoneList);
        tasks.addAll(taskUnDoneList);

        for (OpenListTaskInfo task : tasks) {
            String id = task.getId();
            String name = task.getName();
            if (name.contains(magnet)) {
                log.info("删除残留任务: {} {}", id, name);
                taskDelete(id);
            }
        }
    }

    /**
     * 未完成的离线任务
     *
     * @return 任务列表
     */
    public List<OpenListTaskInfo> taskUnDoneList() {
        return getApi("task/offline_download/undone")
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();
                    return GsonStatic.fromJsonList(jsonArray, OpenListTaskInfo.class);
                });
    }

    /**
     * 已完成的离线任务
     *
     * @return 任务列表
     */
    public List<OpenListTaskInfo> taskDoneList() {
        return getApi("task/offline_download/done")
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();
                    return GsonStatic.fromJsonList(jsonArray, OpenListTaskInfo.class);
                });
    }

    /**
     * 重试任务
     *
     * @param tid 任务id
     */
    public void taskRetry(String tid) {
        postApi("task/offline_download/retry")
                .form("tid", tid)
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 删除任务
     *
     * @param tid 任务id
     */
    public void taskDelete(String tid) {
        postApi("task/offline_download/delete_some")
                .body(GsonStatic.toJson(List.of(tid)))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 获取目录下及子目录的文件
     *
     * @param path 目录
     * @return 文件列表
     */
    public List<OpenListFileInfo> findFiles(String path) {
        List<OpenListFileInfo> openListFileInfos = fsList(path, true);
        List<OpenListFileInfo> list = openListFileInfos.stream()
                .flatMap(openListFileInfo -> {
                    if (openListFileInfo.getIsDir()) {
                        return findFiles(path + "/" + openListFileInfo.getName()).stream();
                    }
                    return Stream.of(openListFileInfo);
                }).toList();

        return ListUtil.sort(new ArrayList<>(list), Comparator.comparing(fileInfo -> {
            Long size = fileInfo.getSize();
            return Long.MAX_VALUE - ObjectUtil.defaultIfNull(size, 0L);
        }));
    }

    /**
     * get api
     *
     * @param action Action
     * @return HttpRequest
     */
    public HttpRequest getApi(String action) {
        ThreadUtil.sleep(2000);
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        return HttpReq.get(host + "/api/" + action)
                .header(Header.AUTHORIZATION, password);
    }

    /**
     * post api
     *
     * @param action Action
     * @return HttpRequest
     */
    public HttpRequest postApi(String action) {
        ThreadUtil.sleep(2000);
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        return HttpReq.post(host + "/api/" + action)
                .header(Header.AUTHORIZATION, password);
    }

}
