package ani.rss;

import ani.rss.action.AniAction;
import ani.rss.action.RootAction;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Config;
import ani.rss.entity.Item;
import ani.rss.util.AniUtil;
import ani.rss.util.ConfigUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.server.SimpleServer;
import cn.hutool.http.server.action.Action;
import cn.hutool.log.Log;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Main {
    static Log log = Log.get(Main.class);

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        String port = env.getOrDefault("PORT", "7789");
        String downloadPath = env.getOrDefault("DOWNLOAD_PATH", "");
        Assert.notBlank(downloadPath, "下载地址不能为空");
        TorrentUtil.downloadPath = downloadPath;
        SimpleServer server = HttpUtil.createServer(Integer.parseInt(port));

        server.addAction("/", new RootAction());
        Set<Class<?>> classes = ClassUtil.scanPackage("ani.rss.action");
        for (Class<?> aClass : classes) {
            Path path = aClass.getAnnotation(Path.class);
            if (Objects.isNull(path)) {
                continue;
            }
            Object action = ReflectUtil.newInstanceIfPossible(aClass);
            server.addAction("/api" + path.value(), (Action) action);
        }

        ThreadUtil.execute(server::start);

        AniUtil.load();
        ConfigUtil.load();
        ThreadUtil.execute(() -> {
            Config config = ConfigUtil.getConfig();
            while (true) {
                String host = config.getHost();
                String username = config.getUsername();
                String password = config.getPassword();

                if (StrUtil.isBlank(host) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
                    continue;
                }

                String s = HttpRequest.post(host + "/api/v2/auth/login")
                        .form("username", username)
                        .form("password", password)
                        .thenFunction(HttpResponse::body);
                if (!s.equals("Ok.")) {
                    log.error("登录 qBittorrent 失败");
                    continue;
                }
                List<Ani> aniList = ObjectUtil.clone(AniAction.aniList);
                for (Ani ani : aniList) {
                    try {
                        List<Item> items = AniUtil.getItems(ani);
                        TorrentUtil.download(ani, items);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
                Integer sleep = config.getSleep();
                ThreadUtil.sleep(sleep, TimeUnit.MINUTES);
            }
        });
    }
}
