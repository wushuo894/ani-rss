package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ExceptionUtil;
import ani.rss.util.GsonStatic;
import ani.rss.util.HttpReq;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            return HttpReq.post(host + "/api/fs/list", false)
                    .header(Header.AUTHORIZATION, password)
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
                        if (!List.of("115 Cloud", "ThunderExpert", "PikPak").contains(provider)) {
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
        String host = config.getHost();
        String password = config.getPassword();
        String magnet = TorrentUtil.getMagnet(torrentFile);
        String reName = item.getReName();
        String path = savePath + "/" + reName;
        try {
            HttpReq.post(host + "/api/fs/add_offline_download", false)
                    .header(Header.AUTHORIZATION, password)
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
            for (int i = 0; i < 60; i++) {
                Thread.sleep(3000);
                if (ls(savePath)
                        .stream()
                        .noneMatch(alistFileInfo -> alistFileInfo.getName().equals(reName))) {
                    continue;
                }
                List<AlistFileInfo> alistFileInfos = ls(path);
                if (alistFileInfos.isEmpty()) {
                    continue;
                }
                String parentName = alistFileInfos.get(0).getName();
                alistFileInfos = ls(path + "/" + parentName);
                if (alistFileInfos.isEmpty()) {
                    continue;
                }
                // 重命名
                List<Map<String, String>> rename_objects = alistFileInfos.stream()
                        .map(alistFileInfo -> {
                            String newName = reName + "." + FileUtil.extName(alistFileInfo.getName());
                            log.info("重命名 {} ==> {}", alistFileInfo.getName(), newName);
                            return Map.of(
                                    "src_name", alistFileInfo.getName(),
                                    "new_name", reName + "." + FileUtil.extName(alistFileInfo.getName())
                            );
                        }).toList();
                HttpReq.post(host + "/api/fs/batch_rename", false)
                        .header(Header.AUTHORIZATION, password)
                        .body(GsonStatic.toJson(Map.of(
                                "src_dir", path + "/" + parentName,
                                "rename_objects", rename_objects
                        ))).then(res -> log.info(res.body()));

                // 移动
                Thread.sleep(2000);
                List<String> names = alistFileInfos.stream()
                        .map(alistFileInfo -> reName + "." + FileUtil.extName(alistFileInfo.getName()))
                        .toList();
                HttpReq.post(host + "/api/fs/recursive_move", false)
                        .header(Header.AUTHORIZATION, password)
                        .body(GsonStatic.toJson(Map.of(
                                "src_dir", path + "/" + parentName,
                                "dst_dir", savePath,
                                "names", names
                        ))).then(res -> log.info(res.body()));

                // 删除残留文件夹
                Thread.sleep(2000);
                HttpReq.post(host + "/api/fs/remove", false)
                        .header(Header.AUTHORIZATION, password)
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
        ThreadUtil.sleep(2000);
        String host = config.getHost();
        String password = config.getPassword();
        return HttpReq.post(host + "/api/fs/list", false)
                .header(Header.AUTHORIZATION, password)
                .body(GsonStatic.toJson(Map.of(
                        "path", path,
                        "page", 1,
                        "per_page", 0,
                        "refresh", false
                )))
                .thenFunction(res -> {
                    JsonObject jsonObject = GsonStatic.fromJson(res.body(), JsonObject.class);
                    JsonArray jsonArray = jsonObject.getAsJsonObject("data")
                            .getAsJsonArray("content");
                    return GsonStatic.fromJsonList(jsonArray, AlistFileInfo.class);
                });
    }

    @Data
    @Accessors(chain = true)
    public static class AlistFileInfo implements Serializable {
        private String name;
        private Long size;
        private Boolean is_dir;
        private Date modified;
        private Date created;
    }

}
