package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.enums.StringEnum;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.collection.ListUtil;
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
    public Boolean login(Config config) {
        this.config = config;
        String host = config.getHost();
        String password = config.getPassword();
        if (StrUtil.isBlank(host) || StrUtil.isBlank(password)) {
            log.warn("Alist 未配置完成");
            return false;
        }
        String downloadPath = config.getDownloadPath();
        Assert.notBlank(downloadPath, "未设置下载位置");
        try {
            return fsApi("list")
                    .body(GsonStatic.toJson(Map.of(
                            "path", downloadPath,
                            "page", 1,
                            "per_page", 0,
                            "refresh", false
                    )))
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
                        JsonObject data = jsonObject.getAsJsonObject("data");

                        boolean write = data.get("write").getAsBoolean();
                        Assert.isTrue(write, "没有写入权限");

                        String provider = data.get("provider").getAsString();
                        if (!List.of("115 Cloud", "Thunder", "PikPak").contains(provider)) {
                            throw new IllegalArgumentException(StrFormatter.format("不支持的网盘类型 {}", provider));
                        }
                        config.setProvider(provider);
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
    public Boolean download(Item item, String savePath, File torrentFile, Boolean ova) {
        String magnet = TorrentUtil.getMagnet(torrentFile);
        String reName = item.getReName();
        String path = savePath + "/" + reName;
        Boolean backRss = config.getBackRss();
        Boolean delete = config.getDelete();
        try {
            // 洗版，删除备 用RSS 所下载的视频
            if (backRss && delete) {
                String s = ReUtil.get(StringEnum.SEASON_REG, reName, 0);
                ls(savePath)
                        .stream()
                        .map(AlistFileInfo::getName)
                        .filter(name -> name.contains(s))
                        .forEach(name -> {
                            fsApi("remove")
                                    .body(GsonStatic.toJson(Map.of(
                                            "dir", savePath,
                                            "names", List.of(name)
                                    ))).then(HttpResponse::isOk);
                            log.info("已开启备用RSS, 自动删除 {}/{}", savePath, name);
                        });
            }
            fsApi("add_offline_download")
                    .body(GsonStatic.toJson(Map.of(
                            "path", path,
                            "urls", List.of(magnet),
                            "tool", config.getProvider(),
                            "delete_policy", "delete_on_upload_succeed"
                    )))
                    .then(res -> {
                        JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                        Assert.isTrue(jsonObject.get("code").getAsInt() == 200, "添加离线下载失败 {}", reName);
                    });
            log.info("添加离线下载成功");
            for (int i = 0; i < 5; i++) {
                Thread.sleep(2000);
                List<AlistFileInfo> alistFileInfos = findFiles(path);

                // 取大小最大的一个视频文件
                AlistFileInfo videoFile = alistFileInfos.stream()
                        .filter(alistFileInfo ->
                                videoFormat.contains(FileUtil.extName(alistFileInfo.getName())))
                        .findFirst()
                        .orElse(null);

                if (Objects.isNull(videoFile)) {
                    continue;
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
                fsApi("batch_rename")
                        .body(GsonStatic.toJson(Map.of(
                                "src_dir", videoFile.getPath(),
                                "rename_objects", rename_objects
                        ))).then(res -> log.info(res.body()));

                // 移动
                List<String> names = renameMap.values()
                        .stream()
                        .toList();
                fsApi("recursive_move")
                        .body(GsonStatic.toJson(Map.of(
                                "src_dir", videoFile.getPath(),
                                "dst_dir", savePath,
                                "names", names
                        ))).then(res -> log.info(res.body()));

                // 删除残留文件夹
                fsApi("remove")
                        .body(GsonStatic.toJson(Map.of(
                                "dir", savePath,
                                "names", List.of(reName)
                        ))).then(HttpResponse::isOk);
                return true;
            }
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

    public List<AlistFileInfo> ls(String path) {
        try {
            return fsApi("list")
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
     * fs api
     *
     * @param action
     * @return
     */
    public synchronized HttpRequest fsApi(String action) {
        ThreadUtil.sleep(2000);
        String host = config.getHost();
        String password = config.getPassword();
        return HttpReq.post(host + "/api/fs/" + action, false)
                .header(Header.AUTHORIZATION, password);
    }

}
