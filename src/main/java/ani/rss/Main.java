package ani.rss;

import ani.rss.action.AniAction;
import ani.rss.action.RootAction;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.entity.Result;
import ani.rss.util.*;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Log LOG = Log.get(Main.class);

    public static void main(String[] args) {
        String version = MavenUtil.getVersion();
        LOG.info("version {}", version);
        ConfigUtil.load();
        AniUtil.load();
        // 处理旧图片
        for (Ani ani : AniAction.getAniList()) {
            String cover = ani.getCover();
            if (!ReUtil.contains("http(s*)://", cover)) {
                continue;
            }
            cover = AniUtil.saveJpg(cover);
            ani.setCover(cover);
            AniUtil.sync();
        }
        ThreadUtil.execute(() -> ServerUtil.create().start());

        ThreadUtil.execute(() -> {
            Config config = ConfigUtil.getCONFIG();
            while (true) {
                Integer sleep = config.getSleep();
                if (!TorrentUtil.login()) {
                    ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
                    continue;
                }
                List<Ani> aniList = ObjectUtil.clone(AniAction.getAniList());
                for (Ani ani : aniList) {
                    try {
                        List<Item> items = AniUtil.getItems(ani);
                        TorrentUtil.downloadAni(ani, items);
                    } catch (Exception e) {
                        LOG.error("{} {}", ani.getTitle(), e.getMessage());
                        LOG.debug(e);
                    }
                    // 避免短时间频繁请求导致流控
                    ThreadUtil.sleep(500);
                }
                ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
            }
        });
    }
}
