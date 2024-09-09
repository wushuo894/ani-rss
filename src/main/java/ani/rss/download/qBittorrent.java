package ani.rss.download;

import ani.rss.entity.Config;
import ani.rss.entity.TorrentsInfo;
import ani.rss.util.ConfigUtil;
import ani.rss.util.HttpReq;
import ani.rss.util.MailUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class qBittorrent implements BaseDownload {

    @Override
    public Boolean login() {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        String username = config.getUsername();
        String password = config.getPassword();
        String downloadPath = config.getDownloadPath();

        if (StrUtil.isBlank(host) || StrUtil.isBlank(username)
                || StrUtil.isBlank(password) || StrUtil.isBlank(downloadPath)) {
            log.warn("qBittorrent 未配置完成");
            return false;
        }

        ThreadUtil.sleep(1000);
        try {
            return HttpReq.post(host + "/api/v2/auth/login", false)
                    .form("username", username)
                    .form("password", password)
                    .setFollowRedirects(true)
                    .thenFunction(res -> {
                        if (!res.isOk() || !res.body().equals("Ok.")) {
                            log.error("登录 qBittorrent 失败");
                            return false;
                        }
                        return true;
                    });
        } catch (Exception e) {
            log.error("登录 qBittorrent 失败 {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<TorrentsInfo> getTorrentsInfos() {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        ThreadUtil.sleep(1000);
        return HttpReq.get(host + "/api/v2/torrents/info", false)
                .thenFunction(res -> {
                    List<TorrentsInfo> torrentsInfoList = new ArrayList<>();
                    JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        TorrentsInfo torrentsInfo = gson.fromJson(jsonObject, TorrentsInfo.class);
                        String tags = torrentsInfo.getTags();
                        if (StrUtil.isBlank(tags)) {
                            continue;
                        }
                        // 包含标签
                        if (StrUtil.split(tags, ",", true, true).contains(tag)) {
                            torrentsInfoList.add(torrentsInfo);
                        }
                    }
                    return torrentsInfoList;
                });
    }

    @Override
    public Boolean download(String name, String savePath, File torrentFile) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        HttpReq.post(host + "/api/v2/torrents/add", false)
                .form("addToTopOfQueue", false)
                .form("autoTMM", false)
                .form("contentLayout", "Original")
                .form("dlLimit", 0)
                .form("firstLastPiecePrio", false)
                .form("paused", false)
                .form("rename", name)
                .form("savepath", savePath)
                .form("sequentialDownload", false)
                .form("skip_checking", false)
                .form("stopCondition", "None")
                .form("upLimit", 102400)
                .form("useDownloadPath", false)
                .form("torrents", torrentFile)
                .form("tags", "ani-rss")
                .thenFunction(HttpResponse::isOk);

        MailUtils.send(StrFormatter.format("{} 已更新", name));
        String hash = FileUtil.mainName(torrentFile);
        // 等待任务添加完成 最多等待10次检测 共30秒
        for (int i = 0; i < 10; i++) {
            ThreadUtil.sleep(3000);
            List<TorrentsInfo> torrentsInfos = getTorrentsInfos();
            Optional<TorrentsInfo> optionalTorrentsInfo = torrentsInfos
                    .stream()
                    .filter(torrentsInfo -> torrentsInfo.getHash().equals(hash))
                    .findFirst();
            if (optionalTorrentsInfo.isPresent()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void delete(TorrentsInfo torrentsInfo) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        Boolean delete = config.getDelete();
        if (!delete) {
            return;
        }
        String hash = torrentsInfo.getHash();
        TorrentsInfo.State state = torrentsInfo.getState();
        String name = torrentsInfo.getName();
        // 下载完成后自动删除任务
        if (EnumUtil.equalsIgnoreCase(state, TorrentsInfo.State.pausedUP.name())) {
            log.info("删除已完成任务 {}", name);
            HttpReq.post(host + "/api/v2/torrents/delete", false)
                    .form("hashes", hash)
                    .form("deleteFiles", false)
                    .thenFunction(HttpResponse::isOk);
        }
    }

    @Override
    public void rename(TorrentsInfo torrentsInfo, String reName) {
        Config config = ConfigUtil.CONFIG;
        String host = config.getHost();
        Boolean rename = config.getRename();
        if (!rename) {
            return;
        }
        if (!ReUtil.contains("S\\d+E\\d+$", reName)) {
            return;
        }
        String hash = torrentsInfo.getHash();
        List<String> nameList = HttpReq.get(host + "/api/v2/torrents/files", false)
                .form("hash", hash)
                .thenFunction(res -> {
                    JsonArray jsonElements = gson.fromJson(res.body(), JsonArray.class);

                    List<String> names = new ArrayList<>();
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        String name = jsonObject.get("name").getAsString();
                        names.add(name);
                    }
                    return names;
                });

        List<String> newNames = new ArrayList<>();

        for (String name : nameList) {
            String ext = FileUtil.extName(name);
            if (StrUtil.isBlank(ext)) {
                continue;
            }
            String newPath = reName;
            if (List.of("mp4", "mkv", "avi", "wmv").contains(ext.toLowerCase())) {
                newPath = newPath + "." + ext;
            } else if (List.of("ass", "ssa", "sub", "srt", "lyc").contains(ext.toLowerCase())) {
                String s = FileUtil.extName(FileUtil.mainName(name));
                if (StrUtil.isNotBlank(s)) {
                    newPath = newPath + "." + s;
                }
                newPath = newPath + "." + ext;
            } else {
                continue;
            }

            if (nameList.contains(newPath)) {
                continue;
            }
            if (newNames.contains(newPath)) {
                continue;
            }
            newNames.add(newPath);

            if (name.equals(newPath)) {
                continue;
            }

            log.info("重命名 {} ==> {}", name, newPath);

            Boolean b = HttpReq.post(host + "/api/v2/torrents/renameFile", false)
                    .form("hash", hash)
                    .form("oldPath", name)
                    .form("newPath", newPath)
                    .thenFunction(HttpResponse::isOk);
            Assert.isTrue(b, "重命名失败 {} ==> {}", name, newPath);
        }
    }
}
