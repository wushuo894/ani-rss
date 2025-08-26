package ani.rss.download;

import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.NotificationStatusEnum;
import ani.rss.enums.StringEnum;
import ani.rss.util.*;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class Alist implements BaseDownload {
    private Config config;

    @Override
    public Boolean login(Boolean test, Config config) {
        this.config = config;
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        if (StrUtil.isBlank(host) || StrUtil.isBlank(password)) {
            log.warn("Alist 未配置完成");
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
                            log.error("登录 Alist 失败");
                            return false;
                        }
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        if (jsonObject.get("code").getAsInt() != 200) {
                            log.error("登录 Alist 失败");
                            return false;
                        }
                        return true;
                    });
        } catch (Exception e) {
            String message = ExceptionUtil.getMessage(e);
            log.error(e.getMessage(), e);
            log.error("登录 Alist 失败 {}", message);
        }
        return false;
    }


    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        return List.of();
    }

    @Override
    public synchronized Boolean download(Ani ani, Item item, String savePath, File torrentFile, Boolean ova) {
        // windows 真该死啊
        savePath = ReUtil.replaceAll(savePath, "^[A-z]:", "");

        String magnet = TorrentUtil.getMagnet(torrentFile);
        String reName = item.getReName();
        String path = savePath + "/" + reName;
        Boolean standbyRss = config.getStandbyRss();
        Boolean delete = config.getDelete();
        Boolean coexist = config.getCoexist();
        try {
            // 洗版，删除备 用RSS 所下载的视频
            if (standbyRss && delete && !coexist) {
                String s = ReUtil.get(StringEnum.SEASON_REG, reName, 0);
                String finalSavePath = savePath;
                ls(savePath)
                        .stream()
                        .map(AlistFileInfo::getName)
                        .filter(name -> name.contains(s))
                        .forEach(name -> {
                            postApi("fs/remove")
                                    .body(GsonStatic.toJson(Map.of(
                                            "dir", finalSavePath,
                                            "names", List.of(name)
                                    ))).then(HttpResponse::isOk);
                            log.info("已开启备用RSS, 自动删除 {}/{}", finalSavePath, name);
                        });
            }
            String tid = postApi("fs/add_offline_download")
                    .body(GsonStatic.toJson(Map.of(
                            "path", path,
                            "urls", List.of(magnet),
                            "tool", config.getProvider(),
                            "delete_policy", "delete_on_upload_succeed"
                    )))
                    .thenFunction(res -> {
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        Assert.isTrue(jsonObject.get("code").getAsInt() == 200, "添加离线下载失败 {}", reName);
                        log.info("添加离线下载成功 {}", reName);
                        return jsonObject.getAsJsonObject("data")
                                .getAsJsonArray("tasks")
                                .get(0).getAsJsonObject()
                                .get("id").getAsString();
                    });

            TimeInterval timer = DateUtil.timer();
            // 重试次数
            long retry = 0;
            while (true) {
                Integer alistDownloadTimeout = config.getAlistDownloadTimeout();
                Long alistDownloadRetryNumber = config.getAlistDownloadRetryNumber();
                if (timer.intervalMinute() > alistDownloadTimeout) {
                    // 超过下载超时限制
                    timer.clear();
                    log.error("{} {} 分钟还未下载完成, 停止检测下载", reName, alistDownloadTimeout);
                    return false;
                }

                // https://github.com/AlistGo/alist/blob/main/pkg/task/task.go
                JsonObject taskInfo;

                try {
                    taskInfo = taskInfo(tid);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    continue;
                }

                String error = taskInfo.get("error").getAsString();
                int state = taskInfo
                        .get("state").getAsInt();
                // errored 重试
                if (state > 5) {
                    // 已到达最大重试次数 5 次, -1 不限制
                    if (alistDownloadRetryNumber > -1) {
                        if (retry >= alistDownloadRetryNumber) {
                            log.error("离线下载失败 {}", error);
                            return false;
                        }
                        retry++;
                        log.info("离线任务正在进行重试 {}, 当前重试次数 {}, 最大重试次数 {}", tid, retry, alistDownloadRetryNumber);
                    }
                    taskRetry(tid);
                    continue;
                }

                if (List.of(3, 4).contains(state)) {
                    log.error("离线任务已被取消 {}", reName);
                    return false;
                }

                // 成功
                if (state == 2) {
                    break;
                }
            }

            if (delete) {
                log.info("离线下载完成, 自动删除已完成任务");
                taskDelete(tid);
            }

            List<AlistFileInfo> alistFileInfos = findFiles(path);

            // 取大小最大的一个视频文件
            AlistFileInfo videoFile = alistFileInfos.stream()
                    .filter(alistFileInfo ->
                            videoFormat.contains(FileUtil.extName(alistFileInfo.getName())))
                    .findFirst()
                    .orElse(null);

            if (Objects.isNull(videoFile)) {
                return false;
            }

            List<AlistFileInfo> subtitleList = alistFileInfos.stream()
                    .filter(alistFileInfo ->
                            subtitleFormat.contains(FileUtil.extName(alistFileInfo.getName())))
                    .toList();

            Map<String, String> renameMap = new HashMap<>();
            renameMap.put(videoFile.getName(), reName + "." + FileUtil.extName(videoFile.getName()));
            for (AlistFileInfo alistFileInfo : subtitleList) {
                String name = alistFileInfo.getName();
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
                List<Map<String, String>> rename_objects = renameMap.entrySet().stream()
                        .map(map -> {
                            String srcName = map.getKey();
                            String newName = map.getValue();
                            log.info("重命名 {} ==> {}", srcName, newName);
                            return Map.of(
                                    "src_name", srcName,
                                    "new_name", newName
                            );
                        }).toList();
                postApi("fs/batch_rename")
                        .body(GsonStatic.toJson(Map.of(
                                "src_dir", videoFile.getPath(),
                                "rename_objects", rename_objects
                        ))).then(res -> log.info(res.body()));
            }

            // 移动
            List<String> names = renameMap.entrySet()
                    .stream()
                    .map(m -> rename ? m.getValue() : m.getKey())
                    .toList();
            postApi("fs/move")
                    .body(GsonStatic.toJson(Map.of(
                            "src_dir", videoFile.getPath(),
                            "dst_dir", savePath,
                            "names", names
                    ))).then(res -> log.info(res.body()));

            // 删除残留文件夹
            postApi("fs/remove")
                    .body(GsonStatic.toJson(Map.of(
                            "dir", savePath,
                            "names", List.of(reName)
                    ))).then(HttpResponse::isOk);

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
    public void rename(TorrentsInfo torrentsInfo) {

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
     * 文件列表
     *
     * @param path
     * @return
     */
    public List<AlistFileInfo> ls(String path) {
        try {
            return postApi("fs/list")
                    .body(GsonStatic.toJson(Map.of(
                            "path", path,
                            "page", 1,
                            "per_page", 0,
                            "refresh", false
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
                        List<AlistFileInfo> infos = GsonStatic.fromJsonList(content.getAsJsonArray(), AlistFileInfo.class);
                        for (AlistFileInfo info : infos) {
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
     * @param tid
     * @return
     */
    public JsonObject taskInfo(String tid) {
        return postApi("task/offline_download/info?tid=" + tid)
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return jsonObject.get("data").getAsJsonObject();
                });
    }

    /**
     * 重试任务
     *
     * @param tid
     */
    public void taskRetry(String tid) {
        postApi("task/offline_download/retry")
                .form("tid", tid)
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 删除任务
     *
     * @param tid
     */
    public void taskDelete(String tid) {
        postApi("task/offline_download/delete_some")
                .body(GsonStatic.toJson(List.of(tid)))
                .thenFunction(HttpResponse::isOk);
    }

    /**
     * 获取目录下及子目录的文件
     *
     * @param path
     * @return
     */
    public synchronized List<AlistFileInfo> findFiles(String path) {
        List<AlistFileInfo> alistFileInfos = ls(path);
        List<AlistFileInfo> list = alistFileInfos.stream()
                .flatMap(alistFileInfo -> {
                    if (alistFileInfo.getIs_dir()) {
                        return findFiles(path + "/" + alistFileInfo.getName()).stream();
                    }
                    return Stream.of(alistFileInfo);
                }).toList();

        return ListUtil.sort(new ArrayList<>(list), Comparator.comparing(fileInfo -> {
            Long size = fileInfo.getSize();
            return Long.MAX_VALUE - ObjectUtil.defaultIfNull(size, 0L);
        }));
    }

    @Data
    @Accessors(chain = true)
    public static class AlistFileInfo implements Serializable {
        private String name;
        private Long size;
        private Boolean is_dir;
        private Date modified;
        private Date created;
        private String path;
    }

    /**
     * post api
     *
     * @param action
     * @return
     */
    public synchronized HttpRequest postApi(String action) {
        ThreadUtil.sleep(2000);
        String host = config.getDownloadToolHost();
        String password = config.getDownloadToolPassword();
        return HttpReq.post(host + "/api/" + action)
                .header(Header.AUTHORIZATION, password);
    }

}
