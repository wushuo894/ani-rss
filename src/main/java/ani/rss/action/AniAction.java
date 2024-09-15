package ani.rss.action;

import ani.rss.annotation.Auth;
import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.util.AniUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.PinyinComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Auth
@Slf4j
@Path("/ani")
public class AniAction implements BaseAction {
    private static final AtomicBoolean download = new AtomicBoolean(false);

    /**
     * 手动刷新订阅
     */
    private void download() {
        Ani ani = getBody(Ani.class);
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getUrl().equals(ani.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            resultError();
            return;
        }
        synchronized (download) {
            if (download.get()) {
                resultErrorMsg("存在未完成任务，请等待...");
                return;
            }
            download.set(true);
            Ani downloadAni = first.get();
            ThreadUtil.execute(() -> {
                try {
                    if (TorrentUtil.login()) {
                        TorrentUtil.downloadAni(downloadAni);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                download.set(false);
            });
            resultSuccessMsg("已开始刷新RSS {}", downloadAni.getTitle());
        }
    }

    /**
     * 添加订阅
     */
    private void post() {
        Ani ani = getBody(Ani.class);
        ani.setTitle(ani.getTitle().trim())
                .setUrl(ani.getUrl().trim());
        AniUtil.verify(ani);

        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getUrl().equals(ani.getUrl()))
                .findFirst();

        if (first.isPresent()) {
            resultErrorMsg("此订阅已存在");
            return;
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();

        if (first.isPresent()) {
            resultErrorMsg("名称重复");
            return;
        }

        AniUtil.ANI_LIST.add(ani);
        AniUtil.sync();
        ThreadUtil.execute(() -> {
            if (TorrentUtil.login()) {
                TorrentUtil.downloadAni(ani);
            }
        });
        resultSuccessMsg("添加订阅成功");
        log.info("添加订阅 {} {}", ani.getTitle(), ani.getUrl());
    }

    /**
     * 修改订阅
     */
    private void put() {
        Ani ani = getBody(Ani.class);
        ani.setTitle(ani.getTitle().trim())
                .setUrl(ani.getUrl().trim());
        AniUtil.verify(ani);
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> !it.getUrl().equals(ani.getUrl()))
                .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                .findFirst();
        if (first.isPresent()) {
            resultErrorMsg("名称重复");
            return;
        }

        first = AniUtil.ANI_LIST.stream()
                .filter(it -> it.getUrl().equals(ani.getUrl()))
                .findFirst();
        if (first.isEmpty()) {
            resultErrorMsg("修改失败");
            return;
        }
        BeanUtil.copyProperties(ani, first.get());
        AniUtil.sync();
        resultSuccessMsg("修改成功");
        log.info("修改订阅 {} {}", ani.getTitle(), ani.getUrl());
    }

    /**
     * 返回订阅列表
     */
    private void get() {
        // 按拼音排序
        PinyinComparator pinyinComparator = new PinyinComparator();
        List<Ani> list = CollUtil.sort(AniUtil.ANI_LIST, (a, b) -> pinyinComparator.compare(a.getTitle(), b.getTitle()));
        for (Ani ani : list) {
            String title = ani.getTitle();
            String pinyin = PinyinUtil.getPinyin(title);
            ani.setPinyin(pinyin);
        }
        resultSuccess(list);
    }

    /**
     * 删除订阅
     */
    public void delete() {
        Ani ani = getBody(Ani.class);
        Optional<Ani> first = AniUtil.ANI_LIST.stream()
                .filter(it -> gson.toJson(it).equals(gson.toJson(ani)))
                .findFirst();
        if (first.isEmpty()) {
            resultError();
            return;
        }
        AniUtil.ANI_LIST.remove(first.get());
        AniUtil.sync();
        resultSuccessMsg("删除订阅成功");
        File torrentDir = TorrentUtil.getTorrentDir(first.get());
        for (File file : FileUtil.loopFiles(torrentDir)) {
            if (file.isDirectory()) {
                continue;
            }
            if (file.getName().endsWith(".txt")) {
                FileUtil.del(file);
            }
            if (file.getName().endsWith(".torrent")) {
                FileUtil.del(file);
            }
        }
        log.info("删除订阅 {} {}", ani.getTitle(), ani.getUrl());
    }

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        String method = req.getMethod();
        String s = req.getParam("download");
        if (Boolean.parseBoolean(s)) {
            download();
            return;
        }

        switch (method) {
            case "POST": {
                post();
                return;
            }
            case "PUT": {
                put();
                return;
            }
            case "GET": {
                get();
                return;
            }
            case "DELETE": {
                delete();
                break;
            }
        }
    }
}
