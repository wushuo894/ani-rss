package ani.rss.util.other;

import ani.rss.commons.GsonStatic;
import ani.rss.config.OpenListConfig;
import ani.rss.entity.OpenListFileInfo;
import ani.rss.entity.web.Header;
import ani.rss.util.basic.HttpReq;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class OpenListUtil {
    private final OpenListConfig openListConfig;

    private OpenListUtil(OpenListConfig openListConfig) {
        this.openListConfig = openListConfig;
    }

    public static OpenListUtil getInstance(String server, String apiKey) {
        return new OpenListUtil(new OpenListConfig(server, apiKey));
    }

    public static OpenListUtil getInstance(OpenListConfig openListConfig) {
        return new OpenListUtil(openListConfig);
    }

    public Boolean test() {
        return postApi("me")
                .thenFunction(HttpResponse::isOk);
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
                )))
                .then(HttpResponse::isOk);
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
     * post api
     *
     * @param action Action
     * @return HttpRequest
     */
    public HttpRequest postApi(String action) {
        ThreadUtil.sleep(2000);
        String server = openListConfig.getServer();
        String apiKey = openListConfig.getApiKey();
        return HttpReq.post(server + "/api/" + action)
                .header(Header.AUTHORIZATION, apiKey);
    }
}
