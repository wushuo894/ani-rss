package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Result;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/ani")
public class AniAction implements Action {
    public static final List<Ani> aniList = new ArrayList<>();
    private final Log log = Log.get(AniAction.class);
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public static File getConfigFile() {
        Map<String, String> env = System.getenv();
        String config = env.getOrDefault("CONFIG", "");
        File configFile = new File("config.json");
        if (StrUtil.isNotBlank(config)) {
            configFile = new File(config + File.separator + "config.json");
        }
        return configFile;
    }

    public static void load() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            FileUtil.writeUtf8String(gson.toJson(aniList), configFile);
        }
        String s = FileUtil.readUtf8String(configFile);
        JsonArray jsonElements = gson.fromJson(s, JsonArray.class);
        for (JsonElement jsonElement : jsonElements) {
            Ani ani = gson.fromJson(jsonElement, Ani.class);
            aniList.add(ani);
        }
    }

    public static void sync() {
        File configFile = getConfigFile();
        String json = gson.toJson(aniList);
        FileUtil.writeUtf8String(JSONUtil.formatJsonStr(json), configFile);
    }


    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) throws IOException {
        String method = req.getMethod();
        res.setContentType("application/json; charset=utf-8");

        try {
            switch (method) {
                case "POST": {
                    Ani ani = gson.fromJson(req.getBody(), Ani.class);
                    ani.setTitle(ani.getTitle().trim())
                            .setUrl(ani.getUrl().trim());
                    Optional<Ani> first = aniList.stream()
                            .filter(it -> it.getUrl().equals(ani.getUrl()) || it.getTitle().equals(ani.getTitle()))
                            .findFirst();
                    if (first.isPresent()) {
                        String json = gson.toJson(Result.error().setMessage("此订阅已存在"));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }
                    synchronized (aniList) {
                        aniList.add(ani);
                        sync();
                    }
                    String json = gson.toJson(Result.success().setMessage("添加订阅成功"));
                    IoUtil.writeUtf8(res.getOut(), true, json);
                    return;
                }
                case "GET": {
                    String json = gson.toJson(Result.success(aniList));
                    IoUtil.writeUtf8(res.getOut(), true, json);
                    return;
                }
                case "DELETE": {
                    Ani ani = gson.fromJson(req.getBody(), Ani.class);
                    Optional<Ani> first = aniList.stream()
                            .filter(it -> gson.toJson(it).equals(gson.toJson(ani)))
                            .findFirst();
                    if (first.isEmpty()) {
                        String json = gson.toJson(Result.error());
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }
                    synchronized (aniList) {
                        aniList.remove(first.get());
                        sync();
                    }
                    String json = gson.toJson(Result.success().setMessage("删除订阅成功"));
                    IoUtil.writeUtf8(res.getOut(), true, json);
                    break;
                }
            }

        } catch (Exception e) {
            log.error(e);
            String json = gson.toJson(Result.error().setMessage(e.getMessage()));
            IoUtil.writeUtf8(res.getOut(), true, json);
        }
    }
}
