package ani.rss.util;

import ani.rss.entity.Config;
import ani.rss.entity.JellyfinViews;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class JellyfinUtil {
    public static String ADMIN_USER_ID = "";

    /**
     * 扫描媒体库
     */
    public static synchronized void refresh(Config config) {
        List<String> viewIds = config.getJellyfinRefreshViewIds();
        List<JellyfinViews> views = getViews(config);

        for (JellyfinViews view : views) {
            String id = view.getId();
            if (viewIds.contains(id)) {
                refresh(view, config);
            }
        }
    }

    /**
     * 扫描媒体库
     *
     * @param JellyfinViews 媒体库
     */
    public static synchronized void refresh(JellyfinViews JellyfinViews, Config config) {
        String JellyfinHost = config.getJellyfinHost();
        String JellyfinApiKey = config.getJellyfinApiKey();

        Assert.notBlank(JellyfinHost, "JellyfinHost 为空");
        Assert.notBlank(JellyfinApiKey, "JellyfinApiKey 为空");

        String s = "?Recursive=true&ImageRefreshMode=Default&MetadataRefreshMode=Default&ReplaceAllImages=false&RegenerateTrickplay=false&ReplaceAllMetadata=false";

        String id = JellyfinViews.getId();
        HttpReq.post(JellyfinHost + "/Items/" + id + "/Refresh?" + s, false)
                .header("Accept", "application/json")
                .header("Authorization", "MediaBrowser Client=\"ani-rss\", Device=\"ani-rss\"," + "Version=\"" + config.getVersion() + "\",Token=\"" + JellyfinApiKey + "\"")
                .then(res -> {
                    if (res.isOk()) {
                        log.info("Jellyfin正在扫描媒体库 id: {} name: {}", JellyfinViews.getId(), JellyfinViews.getName());
                    } else {
                        int status = res.getStatus();
                        log.error("Jellyfin扫描媒体库出错 id: {} name: {} status: {}", JellyfinViews.getId(), JellyfinViews.getName(), status);
                    }
                });
    }

    /**
     * 获取媒体库列表
     *
     * @return 媒体库列表
     */
    public static synchronized List<JellyfinViews> getViews(Config config) {
        String JellyfinHost = config.getJellyfinHost();
        String JellyfinApiKey = config.getJellyfinApiKey();

        Assert.notBlank(JellyfinHost, "JellyfinHost 为空");
        Assert.notBlank(JellyfinApiKey, "JellyfinApiKey 为空");

        List<JellyfinViews> viewsList = new ArrayList<>();

        getAdmin(config);

        JsonArray items = HttpReq.get(JellyfinHost + "/Users/" + ADMIN_USER_ID + "/Items" , false)
                .header("Accept", "application/json")
                .header("Authorization", "MediaBrowser Client=\"ani-rss\", Device=\"ani-rss\"," + "Version=\"" + config.getVersion() + "\",Token=\"" + JellyfinApiKey + "\"")
                .thenFunction(res -> {
                    JsonObject body = GsonStatic.fromJson(res.body(), JsonObject.class);
                    return body.get("Items").getAsJsonArray();
                });

        // 遍历媒体库
        for (JsonElement item : items) {
            JsonObject itemAsJsonObject = item.getAsJsonObject();
            String id = itemAsJsonObject.get("Id").getAsString();
            String name = itemAsJsonObject.get("Name").getAsString();
            if (!Objects.equals(itemAsJsonObject.get("Type").getAsString(), "CollectionFolder")) {continue;}
            JellyfinViews views = new JellyfinViews()
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
        String host = config.getJellyfinHost();
        String JellyfinApiKey = config.getJellyfinApiKey();

        JsonObject adminUser = HttpReq.get(host + "/Users" , false)
                .header("Accept", "application/json")
                .header("Authorization", "MediaBrowser Client=\"ani-rss\", Device=\"ani-rss\"," + "Version=\"" + config.getVersion() + "\",Token=\"" + JellyfinApiKey + "\"")
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
        log.info("Jellyfin adminUserId => {}", ADMIN_USER_ID);
    }

}
