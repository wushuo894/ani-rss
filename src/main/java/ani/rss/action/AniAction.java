package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Item;
import ani.rss.entity.Result;
import ani.rss.util.AniUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Path("/ani")
public class AniAction implements Action {
    public static final List<Ani> aniList = AniUtil.getANI_LIST();
    private final Log log = Log.get(AniAction.class);
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

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
                            .filter(it -> it.getTitle().equals(ani.getTitle()))
                            .findFirst();
                    if (first.isPresent()) {
                        String json = gson.toJson(Result.error().setMessage("名称重复"));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }

                    first = aniList.stream()
                            .filter(it -> it.getUrl().equals(ani.getUrl()))
                            .findFirst();
                    if (first.isPresent()) {
                        String json = gson.toJson(Result.error().setMessage("此订阅已存在"));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }

                    synchronized (aniList) {
                        aniList.add(ani);
                        AniUtil.sync();
                        List<Item> items = AniUtil.getItems(ani);
                        if (TorrentUtil.login()) {
                            TorrentUtil.download(ani, items);
                        }
                    }
                    String json = gson.toJson(Result.success().setMessage("添加订阅成功"));
                    IoUtil.writeUtf8(res.getOut(), true, json);
                    return;
                }
                case "PUT": {
                    Ani ani = gson.fromJson(req.getBody(), Ani.class);
                    ani.setTitle(ani.getTitle().trim())
                            .setUrl(ani.getUrl().trim());

                    Optional<Ani> first = aniList.stream()
                            .filter(it -> !it.getUrl().equals(ani.getUrl()))
                            .filter(it -> it.getTitle().equals(ani.getTitle()))
                            .findFirst();
                    if (first.isPresent()) {
                        String json = gson.toJson(Result.error().setMessage("名称重复"));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }

                    first = aniList.stream()
                            .filter(it -> it.getUrl().equals(ani.getUrl()))
                            .findFirst();
                    if (first.isEmpty()) {
                        String json = gson.toJson(Result.error().setMessage("修改失败"));
                        IoUtil.writeUtf8(res.getOut(), true, json);
                        return;
                    }
                    BeanUtil.copyProperties(ani, first.get());
                    AniUtil.sync();
                    String json = gson.toJson(Result.success().setMessage("修改成功"));
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
                        AniUtil.sync();
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
