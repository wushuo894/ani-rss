package ani.rss.action;

import ani.rss.annotation.Path;
import ani.rss.entity.Ani;
import ani.rss.entity.Result;
import ani.rss.util.AniUtil;
import ani.rss.util.TorrentUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.PinyinComparator;
import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import cn.hutool.http.server.action.Action;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Path("/ani")
public class AniAction implements BaseAction {
    @Getter
    private static final List<Ani> aniList = AniUtil.getANI_LIST();

    @Override
    public void doAction(HttpServerRequest req, HttpServerResponse res) {
        String method = req.getMethod();
        res.setContentType("application/json; charset=utf-8");

        switch (method) {
            case "POST": {
                Ani ani = getBody(Ani.class);
                ani.setTitle(ani.getTitle().trim())
                        .setUrl(ani.getUrl().trim());
                AniUtil.verify(ani);
                Optional<Ani> first = aniList.stream()
                        .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                        .findFirst();
                if (first.isPresent()) {
                    result(Result.error().setMessage("名称重复"));
                    return;
                }

                first = aniList.stream()
                        .filter(it -> it.getUrl().equals(ani.getUrl()))
                        .findFirst();
                if (first.isPresent()) {
                    result(Result.error().setMessage("此订阅已存在"));
                    return;
                }

                aniList.add(ani);
                AniUtil.sync();
                if (TorrentUtil.login()) {
                    TorrentUtil.downloadAni(ani);
                }
                result(Result.success().setMessage("添加订阅成功"));
                return;
            }
            case "PUT": {
                Ani ani = getBody(Ani.class);
                ani.setTitle(ani.getTitle().trim())
                        .setUrl(ani.getUrl().trim());
                AniUtil.verify(ani);
                Optional<Ani> first = aniList.stream()
                        .filter(it -> !it.getUrl().equals(ani.getUrl()))
                        .filter(it -> it.getTitle().equals(ani.getTitle()) && it.getSeason().equals(ani.getSeason()))
                        .findFirst();
                if (first.isPresent()) {
                    result(Result.error().setMessage("名称重复"));
                    return;
                }

                first = aniList.stream()
                        .filter(it -> it.getUrl().equals(ani.getUrl()))
                        .findFirst();
                if (first.isEmpty()) {
                    result(Result.error().setMessage("修改失败"));
                    return;
                }
                BeanUtil.copyProperties(ani, first.get());
                AniUtil.sync();
                result(Result.success().setMessage("修改成功"));
                return;
            }
            case "GET": {
                // 按拼音排序
                PinyinComparator pinyinComparator = new PinyinComparator();
                List<Ani> list = CollUtil.sort(aniList, (a, b) -> pinyinComparator.compare(a.getTitle(), b.getTitle()));
                for (Ani ani : list) {
                    String title = ani.getTitle();
                    String pinyin = PinyinUtil.getPinyin(title);
                    ani.setPinyin(pinyin);
                }
                resultSuccess(list);
                return;
            }
            case "DELETE": {
                Ani ani = getBody(Ani.class);
                Optional<Ani> first = aniList.stream()
                        .filter(it -> gson.toJson(it).equals(gson.toJson(ani)))
                        .findFirst();
                if (first.isEmpty()) {
                    resultError();
                    return;
                }
                aniList.remove(first.get());
                AniUtil.sync();
                result(Result.success().setMessage("删除订阅成功"));
                break;
            }
        }
    }
}
