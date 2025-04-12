package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.EmbyViews;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EmbyUtil {
    public static String ADMIN_USER_ID = "";

    /**
     * 扫描媒体库
     */
    public static synchronized void refresh(Config config) {
        List<String> viewIds = config.getEmbyRefreshViewIds();
        List<EmbyViews> views = getViews(config);

        for (EmbyViews view : views) {
            String id = view.getId();
            if (viewIds.contains(id)) {
                refresh(view, config);
            }
        }
    }

    /**
     * 扫描媒体库
     *
     * @param embyViews 媒体库
     */
    public static synchronized void refresh(EmbyViews embyViews, Config config) {
        String embyHost = config.getEmbyHost();
        String embyApiKey = config.getEmbyApiKey();

        Assert.notBlank(embyHost, "embyHost 为空");
        Assert.notBlank(embyApiKey, "embyApiKey 为空");

        String s = "Recursive=true&ImageRefreshMode=Default&MetadataRefreshMode=Default&ReplaceAllImages=false&ReplaceAllMetadata=false";

        String id = embyViews.getId();
        HttpReq.post(embyHost + "/emby/Items/" + id + "/Refresh?" + s + "&api_key=" + embyApiKey, false)
                .then(res -> {
                    if (res.isOk()) {
                        log.info("Emby正在扫描媒体库 id: {} name: {}", embyViews.getId(), embyViews.getName());
                    } else {
                        int status = res.getStatus();
                        log.error("Emby扫描媒体库出错 id: {} name: {} status: {}", embyViews.getId(), embyViews.getName(), status);
                    }
                });
    }

    /**
     * 获取媒体库列表
     *
     * @return 媒体库列表
     */
    public static synchronized List<EmbyViews> getViews(Config config) {
        String embyHost = config.getEmbyHost();
        String embyApiKey = config.getEmbyApiKey();

        Assert.notBlank(embyHost, "embyHost 为空");
        Assert.notBlank(embyApiKey, "embyApiKey 为空");

        List<EmbyViews> viewsList = new ArrayList<>();

        getAdmin(config);

        JsonArray items = HttpReq.get(embyHost + "/Users/" + ADMIN_USER_ID + "/Views?api_key=" + embyApiKey, false)
                .thenFunction(res -> {
                    JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return body.get("Items").getAsJsonArray();
                });

        // 遍历媒体库
        for (JsonElement item : items) {
            JsonObject itemAsJsonObject = item.getAsJsonObject();
            String id = itemAsJsonObject.get("Id").getAsString();
            String name = itemAsJsonObject.get("Name").getAsString();
            EmbyViews views = new EmbyViews()
                    .setId(id)
                    .setName(name);
            viewsList.add(views);
        }

        return viewsList;
    }

    /**
     * 获取管理员账户
     */
    public static void getAdmin(Config config) {
        if (StrUtil.isNotBlank(ADMIN_USER_ID)) {
            return;
        }
        String host = config.getEmbyHost();
        String key = config.getEmbyApiKey();

        JsonObject adminUser = HttpReq.get(host + "/Users?api_key=" + key, false)
                .thenFunction(res -> {
                    JsonArray jsonElements = GsonStatic.fromJson(res.body(), JsonArray.class);
                    for (JsonElement jsonElement : jsonElements) {
                        JsonObject user = jsonElement.getAsJsonObject();
                        JsonObject policy = user.get("Policy").getAsJsonObject();
                        boolean isAdministrator = policy.get("IsAdministrator").getAsBoolean();
                        if (!isAdministrator) {
                            continue;
                        }
                        return user;
                    }
                    return null;
                });
        Assert.notNull(adminUser, "未找到管理员账户，请检查你的API KEY参数");
        ADMIN_USER_ID = adminUser.get("Id").getAsString();
        log.info("Emby adminUserId => {}", ADMIN_USER_ID);
    }

}
